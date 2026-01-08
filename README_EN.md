[Chinese Document](./README.md)

# GravitySensorLayout
**GravitySensorLayout** is a high-performance, lightweight parallax scrolling container designed specifically for Android developers. It utilizes the device's built-in Rotation Vector Sensor to transform physical tilting into silky-smooth scrolling of child views, bringing a deeply immersive 3D visual experience to your App.

---

## 🌟 Features

* 🚀 **Smooth Performance**: Pre-allocated sensor data buffers based on object pooling principles to completely eliminate GC jitters under high-frequency callbacks, maintaining high frame rates even on 120Hz displays.
* 🔄 **Lifecycle Awareness**: Built-in automatic lifecycle management. Sensors are enabled/disabled automatically as the View attaches/detaches, eliminating the need for manual registration logic in Activities.
* 📐 **Screen Rotation Adaptation**: Intelligently handles screen orientation changes (0°/90°/180°/270°) by automatically remapping coordinate axes, ensuring the tilt response always feels intuitive.
* 🎨 **Parallax Nesting Optimization**: Fully supports `normal` (gravity-following) and `reverse` (anti-gravity) modes, making it easy to achieve multi-layered, deep perspective effects.

---

## 🛠 Custom Attributes

| Attribute | Description | Default Value |
| :--- | :--- | :--- |
| `app:gsl_maxHorizontalOffset` | Maximum horizontal scrolling displacement | 150dp |
| `app:gsl_maxVerticalOffset` | Maximum vertical scrolling displacement | 130dp |
| `app:gsl_scrollDuration` | Duration of the smooth scroll animation | 250ms |
| `app:gsl_sensorThreshold` | Tilt angle required to trigger maximum displacement | 45.0 |
| `app:gsl_scrollDirection` | Scrolling direction (`normal` or `reverse`) | `normal` |

---

## Installation

### Gradle:

1. Add the remote repository to your Project's **build.gradle** or **settings.gradle**:

    ```gradle
    repositories {
        mavenCentral()
    }
    ```

2. Add the dependency to your Module's **build.gradle**:
   [![Maven Central](https://img.shields.io/maven-central/v/io.github.logan0817/GravitySensorLayout.svg?label=Latest%20Release)](https://central.sonatype.com/artifact/io.github.logan0817/GravitySensorLayout)

    ```gradle
    implementation 'io.github.logan0817:GravitySensorLayout:1.0.1' // Replace with the version shown on the badge above
    ```

---

## Demo

<img src="GIF.gif" width="350" />

> Demo.apk [Download here](apk/app-debug.apk)

---

## Usage

In your layout file, simply wrap your View (it is recommended that the child View is larger than the container and centered):

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


## Advanced usage

## 1. Nested layouts can be used to create parallax effects by nesting two layouts in opposite directions, resulting in a strong sense of "floating." See the demo for an example.
#### You can control the size of the outer GravitySensorLayout and the inner child View yourself, or use android:clipChildren="false" android:clipToPadding="false" to prevent the child View content from being clipped.

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

### 2. By monitoring the tilt ratio, you can use code to monitor the tilt ratio and achieve more complex lighting or 3D effects:

    val sensorLayout = findViewById<GravitySensorLayout>(R.id.sensorLayout)
    sensorLayout.tiltListener = object : GravitySensorLayout.OnSensorTiltListener {
    override fun onTilt(xRatio: Float, yRatio: Float) {
    // xRatio/yRatio ranges from [-1.0, 1.0]
    // Negative values ​​represent tilting left/forward, positive values ​​represent tilting right/backward
    }
    }

## Support

- If you have any questions, feel free to leave a comment.
- If this project helps you, please consider giving it a ⭐ Star to support the author.

---

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
