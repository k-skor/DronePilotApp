package com.example.dronepilotapp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Surface
import com.example.dronepilotapp.log.LogUtil
import com.example.dronepilotapp.matrix.MatrixCalculations
import com.example.dronepilotapp.renderer.GLRenderer
import com.example.dronepilotapp.sensor.GyroSensor
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


class MainActivity : AppCompatActivity(), SensorEventListener {

    private val LOG = LogUtil.getLogger(javaClass)

    private val degConst = 180f/Math.PI
    private var screenRot: Int = 0

    private lateinit var mSensorManager: SensorManager
    private var mRotationVectorSensor: Sensor? = null
    private var mGravitySensor: Sensor? = null
    private var mMagneticSensor: Sensor? = null
    private var mGyroscopeSensor: Sensor? = null
    private lateinit var mRenderer: GLRenderer
    private lateinit var mGLSurfaceView: GLSurfaceView
    private lateinit var mGyroSensor: GyroSensor
    private val mGravityVector: FloatArray = FloatArray(3)
    private val mMagneticVecotr: FloatArray = FloatArray(3)
    private var mHasGravity = false
    private var mHasMagnetic = false

    private var mTempRotationMatrix = MatrixCalculations.createUnit(3)

    private fun formatMatrix(matrix: FloatArray): String {
        return "[" + matrix.joinToString(" ") { String.format(Locale.US, "%.02f", it) } + "]"
    }

    private fun copyAsset(name: String): File {
        val objFile = createTempFile()
        val output = FileOutputStream(objFile)
        assets.open(name).use { input ->
            do {
                val char = input.read()
                output.write(char)
            } while (char != -1)
        }
        return objFile
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        if (mGravitySensor != null) {
            LOG.info("has gravity sensor")
        }

        val accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if (accelerometer != null) {
            LOG.info("has accelerometer")
        }

        mMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (mMagneticSensor != null) {
            LOG.info("has magnetic")
        }

        mGyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (mGyroscopeSensor != null) {
            LOG.info("has gyroscope")
            mGyroSensor = GyroSensor()
        }

        mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (mRotationVectorSensor != null) {
            LOG.info("has rotation")
        }

        mRenderer = GLRenderer(copyAsset("tinker.obj"))
        mGLSurfaceView = GLSurfaceView(this)
        mGLSurfaceView.setRenderer(mRenderer)
        setContentView(mGLSurfaceView)

        screenRot = windowManager.defaultDisplay.rotation

        var rx = MatrixCalculations.createUnit(3)
        var ry = MatrixCalculations.createUnit(3)
        var rz = MatrixCalculations.createUnit(3)
        // Alpha = 30
        var sina = 0.5f
        var cosa = (Math.sqrt(3.0)/2f).toFloat()
        // Beta = 45
        var sinb = (Math.sqrt(2.0)/2f).toFloat()
        var cosb = sinb
        // Gamma = 60
        var siny = cosa
        var cosy = sina
        rx[4] = cosa; rx[5] = -sina
        rx[7] = sina; rx[8] = cosa
        ry[0] = cosb; ry[2] = sinb
        ry[6] = -sinb; ry[8] = cosb
        rz[0] = cosy; rz[1] = -siny
        rz[3] = siny; rz[4] = cosy
        LOG.info("x= " + formatMatrix(rx))
        LOG.info("y= " + formatMatrix(ry))
        LOG.info("z= " + formatMatrix(rz))
        mTempRotationMatrix = rz
        val rxy = MatrixCalculations.multiply(rx, ry)
        mTempRotationMatrix = MatrixCalculations.multiply(rxy, rz)
        LOG.info(formatMatrix(mTempRotationMatrix))
        val orientation = FloatArray(3)
        SensorManager.getOrientation(mTempRotationMatrix, orientation)

        LOG.info("[1] yaw: " + orientation[0] * degConst + "\n\tpitch: " + orientation[1] * degConst + "\n\troll: " + orientation[2] * degConst)

        // Alpha = 30
        val atanx = -atan2(mTempRotationMatrix[5], mTempRotationMatrix[8])
        sina = sin(atanx)
        cosa = cos(atanx)
        // Beta = 45
        val asiny = -asin(-mTempRotationMatrix[2])
        sinb = sin(asiny)
        cosb = cos(asiny)
        // Gamma = 60
        val atanz = -atan2(mTempRotationMatrix[1], mTempRotationMatrix[0])
        siny = sin(atanz)
        cosy = cos(atanz)
        LOG.info(String.format("angles (a, b, y): (%.1f, %.1f, %.1f)", atanx * degConst, asiny * degConst, atanz * degConst))
        rx = MatrixCalculations.createUnit(3)
        ry = MatrixCalculations.createUnit(3)
        rz = MatrixCalculations.createUnit(3)
        rx[4] = cosa; rx[5] = -sina
        rx[7] = sina; rx[8] = cosa
        ry[0] = cosb; ry[2] = sinb
        ry[6] = -sinb; ry[8] = cosb
        rz[0] = cosy; rz[1] = -siny
        rz[3] = siny; rz[4] = cosy
        val ryz = MatrixCalculations.multiply(rx, ry)
        LOG.info("vector: " + formatMatrix(rx))
        mTempRotationMatrix = MatrixCalculations.multiply(ryz, rz)
        SensorManager.getOrientation(mTempRotationMatrix, orientation)

        LOG.info("[2] yaw: " + orientation[0] * degConst + "\n\tpitch: " + orientation[1] * degConst + "\n\troll: " + orientation[2] * degConst)
        LOG.info(formatMatrix(mTempRotationMatrix))
    }

    override fun onResume() {
        super.onResume()
        mGLSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mGLSurfaceView.onPause()
    }

    override fun onStart() {
        super.onStart()
        mSensorManager.registerListener(this, mRotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL)
        //mSensorManager.registerListener(this, mGravitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        //mSensorManager.registerListener(this, mMagneticSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onStop() {
        super.onStop()
        mSensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        LOG.info("onAccuracyChanged")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val sensor = event?.sensor ?: return
        when (sensor.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, mMagneticVecotr, 0, event.values.size)
                mHasMagnetic = true
            }
            Sensor.TYPE_GRAVITY -> {
                System.arraycopy(event.values, 0, mGravityVector, 0, event.values.size)
                mHasGravity = true
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(mTempRotationMatrix, event.values)
                screenRot = 90
                when (screenRot) {
                    Surface.ROTATION_0 -> {
                        //LOG.info("rotation 0")
                        SensorManager.remapCoordinateSystem(mTempRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, mTempRotationMatrix)
                    }
                    Surface.ROTATION_90 -> {
                        //LOG.info("rotation 90")
                        SensorManager.remapCoordinateSystem(mTempRotationMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, mTempRotationMatrix)
                    }
                    Surface.ROTATION_180 -> {
                        //LOG.info("rotation 180")
                        SensorManager.remapCoordinateSystem(mTempRotationMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, mTempRotationMatrix)
                    }
                    Surface.ROTATION_270 -> {
                        //LOG.info("rotation 270")
                        SensorManager.remapCoordinateSystem(mTempRotationMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, mTempRotationMatrix)
                    }
                }
                //LOG.info("rotation: " + formatMatrix(mTempRotationMatrix))
                val orientation = FloatArray(3)
                SensorManager.getOrientation(mTempRotationMatrix, orientation)
                //LOG.info("orientation yaw: " + orientation[0] * degConst + "\n\tpitch: " + orientation[1] * degConst + "\n\troll: " + orientation[2] * degConst)

                val rx = MatrixCalculations.createUnit(3)
                val ry = MatrixCalculations.createUnit(3)
                val rz = MatrixCalculations.createUnit(3)
                var pitch = if (screenRot == 0) asin(-mTempRotationMatrix[7]) else -atan2(-mTempRotationMatrix[6], mTempRotationMatrix[8])
                var roll = if (screenRot == 0) -atan2(-mTempRotationMatrix[6], mTempRotationMatrix[8]) else asin(-mTempRotationMatrix[7])
                var yaw = atan2(-mTempRotationMatrix[1], mTempRotationMatrix[4])
                //LOG.info(String.format("angles (a, b, y): (%.1f, %.1f, %.1f)", pitch * degConst, roll * degConst, yaw * degConst))

                val q = FloatArray(4)
                SensorManager.getQuaternionFromVector(q, event.values)
                //LOG.info("quaternion: " + formatMatrix(q))

                // mitigate the gimbal lock problem
                val test = q[3] * q[2] + q[1] * q[0]
                if (test > 0.499f) { // singularity at north pole
                    LOG.info("north pole")
                    yaw = 2 * atan2(q[3], q[0])
                    pitch = if (screenRot == 0) -Math.PI.toFloat() / 2 else 0f
                    roll = if (screenRot == 0) 0f else -Math.PI.toFloat() / 2
                }
                if (test < -0.499f) { // singularity at south pole
                    LOG.info("south pole")
                    yaw = -2 * atan2(q[3], q[0])
                    pitch = if (screenRot == 0) Math.PI.toFloat() / 2 else 0f
                    roll = if (screenRot == 0) 0f else Math.PI.toFloat()
                }
                val sina = sin(pitch)
                val cosa = cos(pitch)
                val sinb = sin(roll)
                val cosb = cos(roll)
                val siny = sin(yaw)
                val cosy = cos(yaw)
                rx[4] = cosa; rx[5] = -sina
                rx[7] = sina; rx[8] = cosa
                ry[0] = cosb; ry[2] = sinb
                ry[6] = -sinb; ry[8] = cosb
                rz[0] = cosy; rz[1] = -siny
                rz[3] = siny; rz[4] = cosy

                val rxy = MatrixCalculations.multiply(rx, ry)
                mTempRotationMatrix = rxy
                MatrixCalculations.copy(mTempRotationMatrix, mRenderer.rotationMatrix)

                SensorManager.getOrientation(mTempRotationMatrix, orientation)

                //LOG.info("yaw: " + orientation[0] * degConst + "\n\tpitch: " + orientation[1] * degConst + "\n\troll: " + orientation[2] * degConst)
            }
            Sensor.TYPE_GYROSCOPE -> {
                LOG.info("vector: [" + event.values.joinToString(" ") + "]")
                val deltaRotationMatrix = FloatArray(16)
                mGyroSensor.handleVector(event, deltaRotationMatrix)
                LOG.info("rotation: [" + mRenderer.rotationMatrix.joinToString(" ") + "]" )
                mRenderer.rotationMatrix = MatrixCalculations.multiply(mRenderer.rotationMatrix, deltaRotationMatrix)
                LOG.info("product: [" + mRenderer.rotationMatrix.joinToString(" ") + "]")
            }
        }
        if (mHasGravity && mHasMagnetic) {
            LOG.info("has gravity & magnetic")
            val inclinationMatrix = FloatArray(16)
            SensorManager.getRotationMatrix(
                mRenderer.rotationMatrix,
                inclinationMatrix,
                mGravityVector,
                mMagneticVecotr
            )
            LOG.info("rotation: [" + mRenderer.rotationMatrix.joinToString(" ") + "]")
            val angleRad = SensorManager.getInclination(inclinationMatrix)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(mRenderer.rotationMatrix, orientation)

            LOG.info(
                "yaw: " + ((orientation[0] * degConst + 360) % 360) + "\n" +
                        "pitch: " + orientation[1] * degConst + "\n" +
                        "roll: " + orientation[2] * degConst + "\n" +
                        "inclination: " + angleRad * degConst
            )

        }
    }
}
