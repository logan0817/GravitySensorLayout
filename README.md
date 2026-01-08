英文文档 [English Document](./README_EN.md)

# GravitySensorLayout

**GravitySensorLayout** 是一款专为 Android 开发者设计的高性能、轻量级视差滚动容器。它利用手机内置的旋转矢量传感器（Rotation
Vector Sensor），将设备的物理倾斜实时转化为子 View 的丝滑滚动，为你的 App 带来极具深度感的 3D 视觉体验。

---

## 🌟 特性

* 🚀 **丝滑性能**：采用对象池思想预分配传感器数据缓冲区，彻底杜绝高频回调下的 GC 抖动，即便在 120Hz
  屏幕下也能保持极高帧率。
* 🔄 **生命周期感应**：内置生命周期自动管理，随 View 的附着与脱离自动开启/关闭传感器，无需手动在
  Activity 中写注册逻辑。
* 📐 **屏幕旋转适配**：智能处理设备横竖屏切换（0°/90°/180°/270°），自动重映射坐标轴，确保感应方向始终符合直觉。
* 🎨 **视差嵌套优化**：完美支持 `normal`（随重力动）和 `reverse`（反向动）两种模式，轻松实现多层重叠的深度透视效果。

## 🛠 自定义属性 (Custom Attributes)

| 属性名                           | 说明                          | 默认值      |
|:------------------------------|:----------------------------|:---------|
| `app:gsl_maxHorizontalOffset` | 水平方向最大滚动位移                  | 150dp    |
| `app:gsl_maxVerticalOffset`   | 垂直方向最大滚动位移                  | 130dp    |
| `app:gsl_scrollDuration`      | 平滑滚动动画时长                    | 250ms    |
| `app:gsl_sensorThreshold`     | 触发最大位移的倾斜角度                 | 45.0     |
| `app:gsl_scrollDirection`     | 滚动方向 (`normal` 或 `reverse`) | `normal` |

---

## 引入

### Gradle:

1. 在Project的 **build.gradle** 或 **setting.gradle** 中添加远程仓库

    ```gradle
    repositories {
        //
        mavenCentral()
    }
    ```

2. 在Module的 **build.gradle** 中添加依赖项
   [![Maven Central](https://img.shields.io/maven-central/v/io.github.logan0817/GravitySensorLayout.svg?label=Latest%20Release)](https://central.sonatype.com/artifact/io.github.logan0817/GravitySensorLayout)

    ```gradle
   implementation 'io.github.logan0817:GravitySensorLayout:1.0.2' // 替换为上方徽章显示的最新版本
    ```

## 效果展示

<img src="GIF.gif" width="350" />

> Demo.apk [点击下载](apk/app-debug.apk)

## 在布局文件中，直接包裹你的 View。**注意：为了实现位移效果，内部子 View
的尺寸应当大于 `GravitySensorLayout` 并建议设置居中。**

    <com.logan.view.GravitySensorLayout
        android:layout_width="match_parent"
        android:layout_height="300dp"
        app:gsl_maxHorizontalOffset="60dp"
        app:gsl_scrollDirection="normal">
    
        <ImageView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:layout_gravity="center"
            android:scaleType="centerCrop"
            android:scaleX="1.3"
            android:scaleY="1.3"
            android:src="@mipmap/bg_test_img"  />
    
    </com.logan.view.GravitySensorLayout>


    <com.logan.view.GravitySensorLayout
        android:layout_width="500dp"
        android:layout_height="300dp"
        app:gsl_maxHorizontalOffset="50dp"
        app:gsl_maxVerticalOffset="50dp"
        app:gsl_scrollDirection="normal">
    
        <ImageView
            android:layout_width="600dp"
            android:layout_height="400dp"
            android:layout_gravity="center"
            android:scaleType="centerCrop"
            android:src="@mipmap/bg_test_img"  />
    
    </com.logan.view.GravitySensorLayout>

## 进阶用法

## 1. 嵌套实现视差特效，通过嵌套两个方向相反的布局，可以营造出极强的“悬浮”感：可以查看demo
#### 对于外部GravitySensorLayout和内部子View大小，你可以自己控制两个View大小，或者使用 android:clipChildren="false" android:clipToPadding="false"，来让子View内容不被裁剪。

        <com.logan.view.GravitySensorLayout
            android:layout_width="match_parent"
            android:layout_height="200dp"
            android:clipChildren="false"
            android:clipToPadding="false"
            app:gsl_maxHorizontalOffset="40dp"
            app:gsl_maxVerticalOffset="40dp"
            app:gsl_scrollDirection="normal">

            <androidx.appcompat.widget.AppCompatImageView
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:layout_gravity="center"
                android:scaleType="centerCrop"
                android:scaleX="1.3"
                android:scaleY="1.3"
                android:src="@mipmap/bg_test_img" />

            <com.logan.view.GravitySensorLayout
                android:layout_width="120dp"
                android:layout_height="120dp"
                android:layout_gravity="center"
                android:clipChildren="false"
                android:clipToPadding="false"
                app:gsl_maxHorizontalOffset="60dp"
                app:gsl_maxVerticalOffset="50dp"
                app:gsl_scrollDirection="reverse">

                <androidx.appcompat.widget.AppCompatImageView
                    android:layout_width="80dp"
                    android:layout_height="80dp"
                    android:layout_gravity="center"
                    android:scaleType="centerCrop"
                    android:scaleX="1.3"
                    android:scaleY="1.3"
                    android:src="@mipmap/ic_test_product" />

            </com.logan.view.GravitySensorLayout>

        </com.logan.view.GravitySensorLayout>

### 2. 监听倾斜比例,你可以通过代码监听倾斜比例，以联动实现更复杂的光影或 3D 效果：

    val sensorLayout = findViewById<GravitySensorLayout>(R.id.sensorLayout)
    sensorLayout.tiltListener = object : GravitySensorLayout.OnSensorTiltListener {
    override fun onTilt(xRatio: Float, yRatio: Float) {
    // xRatio/yRatio 范围为 [-1.0, 1.0]
    // 负值代表向左/向前倾斜，正值代表向右/向后倾斜
    }
    }

### 如果你有任何疑问可以留言。

### 如果对你有帮助，可以赏个star支持一下作者。

### License

```
MIT License

Copyright (c) 2025 Logan Gan

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
