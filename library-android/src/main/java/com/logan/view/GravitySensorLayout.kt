package com.logan.view

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.AttributeSet
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Scroller
import com.logan.view.GravitySensorLayout.Companion.DIRECTION_NORMAL
import com.logan.view.GravitySensorLayout.Companion.DIRECTION_REVERSE
import kotlin.math.abs

/**
 * GravitySensorLayout 是一个基于 Android 物理传感器的视差滚动容器。
 *
 * 它通过监听设备的旋转矢量传感器（Rotation Vector Sensor），将设备的物理倾斜实时转换为内部子 View 的滚动偏移量。
 * 开发者可以利用此布局轻松实现类似 3D 壁纸、视差海报或具有深度感的悬浮 UI 效果。
 *
 * ### 主要特性：
 * - **自动资源管理**：内部通过监听可见性与生命周期，自动开关传感器，无需外部干预。
 * - **高性能**：采用零对象分配原则处理高频传感器回调，确保在 120Hz 刷新率下依然流畅。
 * - **全向适配**：内置屏幕旋转重映射逻辑，完美支持横竖屏及各种持握姿势。
 *
 *
 *
 * @author Logan
 * @version 1.0.1
 */
class GravitySensorLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), SensorEventListener {

    /** 传感器管理服务 */
    private val sensorManager: SensorManager? by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }

    /** 窗口管理服务，用于获取屏幕旋转状态 */
    private val windowManager: WindowManager? by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    }

    /** 平滑滚动辅助工具 */
    private val scroller: Scroller = Scroller(context)

    /** 传感器监听注册状态标记 */
    private var isSensorRegistered = false

    /** 预分配旋转矩阵空间，避免高频 GC */
    private val rawRotationMatrix = FloatArray(9)

    /** 预分配适配旋转后的矩阵空间 */
    private val adjustedRotationMatrix = FloatArray(9)

    /** 预分配欧拉角空间 [方位角, 俯仰角, 翻滚角] */
    private val orientationAngles = FloatArray(3)

    /** 水平方向最大滚动偏移量 (单位: px) */
    var maxHorizontalOffset = 150f

    /** 垂直方向最大滚动偏移量 (单位: px) */
    var maxVerticalOffset = 130f

    /** 滚动平滑过渡的持续时间 (单位: ms) */
    var scrollDuration = 0

    /** 传感器响应阈值角度。倾斜达到此角度时达到最大位移 (建议范围: 30.0 - 60.0) */
    var sensorThreshold = 45.0

    /** 滚动方向系数。参见 [DIRECTION_NORMAL] 和 [DIRECTION_REVERSE] */
    var scrollDirection = DIRECTION_NORMAL

    /** 目标位置 X 轴缓存 */
    private var lastTargetX = 0

    /** 目标位置 Y 轴缓存 */
    private var lastTargetY = 0

    /** 设备倾斜比例回调，范围从 -1.0 到 1.0 */
    var tiltListener: OnSensorTiltListener? = null

    companion object {
        /** 常规模式：模拟重力流动物体，手机左偏内容左滑。 */
        const val DIRECTION_NORMAL = 1

        /** 反向模式：模拟 3D 深度视差，手机左偏内容右滑。 */
        const val DIRECTION_REVERSE = -1
    }

    init {
        // 初始化时解析 XML 定义的自定义属性
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.GravitySensorLayout)
        try {
            maxHorizontalOffset = typedArray.getDimension(R.styleable.GravitySensorLayout_gsl_maxHorizontalOffset, 150f)
            maxVerticalOffset = typedArray.getDimension(R.styleable.GravitySensorLayout_gsl_maxVerticalOffset, 130f)
            scrollDuration = typedArray.getInt(R.styleable.GravitySensorLayout_gsl_scrollDuration, 0)
            sensorThreshold = typedArray.getFloat(R.styleable.GravitySensorLayout_gsl_sensorThreshold, 45f).toDouble()
            scrollDirection = typedArray.getInt(R.styleable.GravitySensorLayout_gsl_scrollDirection, DIRECTION_NORMAL)
        } finally {
            typedArray.recycle()
        }
    }

    /**
     * 统一的状态检查方法。
     * 只有当：View已附着 + 窗口可见 + View自身可见 时，才开启传感器。
     */
    private fun updateSensorState() {
        // isShown 会递归检查自身及所有父 View 是否可见
        // windowVisibility == VISIBLE 确保当前窗口（Activity）在前台
        val shouldRegister = isAttachedToWindow && (windowVisibility == VISIBLE) && isShown

        if (shouldRegister) {
            registerSensor()
        } else {
            unregisterSensor()
        }
    }

    /**
     * API 24+ 推荐使用此方法。
     * 它能更智能地汇总可见性变化，避免 onWindowVisibilityChanged 的频繁回调和误判。
     */
    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) {
            registerSensor()
        } else {
            unregisterSensor()
        }
    }

    /**
     * 兼容 API < 24 的情况，或者处理特殊的可见性变化。
     * 使用 updateSensorState() 替代直接调用 register/unregister。
     */
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        // 只有在低版本或者 onVisibilityAggregated 未覆盖的情况下，这个方法作为补充
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            updateSensorState()
        }
    }

    /**
     * 当 View 附着到窗口时触发。
     * 配合可见性判断，确保在界面显示时立即开始感应。
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateSensorState()
    }

    /**
     * 当 View 从当前窗口彻底分离时触发。
     * 彻底释放传感器监听，防止内存泄漏。
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 分离时强制注销，不使用 updateSensorState 以避免任何歧义
        unregisterSensor()
    }

    /**
     * 注册传感器监听器。
     * 使用 [Sensor.TYPE_ROTATION_VECTOR] 提供最稳定和精准的姿态感知。
     */
    fun registerSensor() {
        // 双重保险：如果已经注册，直接返回，避免重复调用系统API
        if (isSensorRegistered) return
        sensorManager?.let {
            val sensor = it.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            // 只有当设备有该传感器时才注册
            if (sensor != null) {
                it.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
                isSensorRegistered = true
            }
        }
    }

    /**
     * 注销传感器监听器，重置注册标记位。
     */
    fun unregisterSensor() {
        // 双重保险：如果没有注册，直接返回
        if (!isSensorRegistered) return
        sensorManager?.unregisterListener(this)
        isSensorRegistered = false
    }

    /**
     * 传感器数据变动回调。
     * 实现 [SensorEventListener] 接口。
     */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            // 获取旋转矩阵
            SensorManager.getRotationMatrixFromVector(rawRotationMatrix, event.values)

            // 修正屏幕旋转导致的轴线偏移
            remapCoordinatesByRotation()

            // 获取方向角数据
            SensorManager.getOrientation(adjustedRotationMatrix, orientationAngles)

            // 将弧度转换为角度
            // orientationAngles[1] 为 Pitch (俯仰角)，控制上下滚动
            // orientationAngles[2] 为 Roll (翻滚角)，控制左右滚动
            val pitchDegrees = Math.toDegrees(orientationAngles[1].toDouble())
            val rollDegrees = Math.toDegrees(orientationAngles[2].toDouble())

            processFinalScroll(pitchDegrees, rollDegrees)
        }
    }

    /**
     * 坐标系重映射逻辑。
     * 根据设备当前物理旋转状态（Surface.ROTATION_X）映射传感器的 X、Y 轴。
     */
    private fun remapCoordinatesByRotation() {
        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.rotation
        } else {
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay?.rotation
        } ?: Surface.ROTATION_0
        var worldAxisX = SensorManager.AXIS_X
        var worldAxisY = SensorManager.AXIS_Y

        when (rotation) {
            Surface.ROTATION_90 -> {
                worldAxisX = SensorManager.AXIS_Y
                worldAxisY = SensorManager.AXIS_MINUS_X
            }

            Surface.ROTATION_180 -> {
                worldAxisX = SensorManager.AXIS_MINUS_X
                worldAxisY = SensorManager.AXIS_MINUS_Y
            }

            Surface.ROTATION_270 -> {
                worldAxisX = SensorManager.AXIS_MINUS_Y
                worldAxisY = SensorManager.AXIS_X
            }
        }
        SensorManager.remapCoordinateSystem(rawRotationMatrix, worldAxisX, worldAxisY, adjustedRotationMatrix)
    }

    /**
     * 计算并执行最终的位移逻辑。
     * @param pitch 设备的俯仰角 (点头动作)
     * @param roll 设备的翻滚角 (侧倾动作)
     */
    private fun processFinalScroll(pitch: Double, roll: Double) {
        val threshold = if (sensorThreshold == 0.0) 1.0 else sensorThreshold

        // 计算倾斜比例，限制在 [-1.0, 1.0]
        val xRatio = (roll.coerceIn(-threshold, threshold) / threshold).toFloat()
        val yRatio = (pitch.coerceIn(-threshold, threshold) / threshold).toFloat()

        // 统一计算映射因子：
        // 1. scrollDirection 决定是随动还是视差
        // 2. -1f 用于对齐 Android scrollTo 坐标系（正值内容向左/上移）
        val baseFactor = scrollDirection.toFloat() * -1f

        val targetX = (xRatio * maxHorizontalOffset * baseFactor).toInt()
        val targetY = (yRatio * maxVerticalOffset * baseFactor).toInt()

        // 差值检测，过滤极细微抖动，提高渲染效率
        if (targetX != lastTargetX || targetY != lastTargetY) {
            lastTargetX = targetX
            lastTargetY = targetY

            val dx = targetX - scrollX
            val dy = targetY - scrollY

            if (abs(dx) >= 1 || abs(dy) >= 1) {
                // 启动弹性平滑滚动
                scroller.startScroll(scrollX, scrollY, dx, dy, scrollDuration)
                invalidate()
            }

            // 触发外部回调
            tiltListener?.onTilt(xRatio, yRatio)
        }
    }

    /**
     * 系统绘制回调，由 invalidate() 触发。
     * 计算 Scroller 的当前偏移量并执行 scrollTo。
     */
    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            postInvalidateOnAnimation()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 暂不处理传感器精度变化
    }

    /**
     * 外部事件监听接口
     */
    interface OnSensorTiltListener {
        /**
         * 当设备倾斜发生变化时回调
         * @param xRatio 水平倾斜比例 [-1.0..1.0]
         * @param yRatio 垂直倾斜比例 [-1.0..1.0]
         */
        fun onTilt(xRatio: Float, yRatio: Float)
    }
}