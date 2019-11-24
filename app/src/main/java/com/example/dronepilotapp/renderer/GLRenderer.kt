package com.example.dronepilotapp.renderer

import android.opengl.GLSurfaceView
import com.example.dronepilotapp.log.LogUtil
import org.j3d.geom.GeometryData
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import org.j3d.loaders.obj.OBJFileReader
import java.io.File


class GLRenderer(objFileName: File?) : GLSurfaceView.Renderer {
    private val LOG = LogUtil.getLogger(javaClass)

    private var cube: Cube
    var rotationMatrix = FloatArray(16)

    init {
        rotationMatrix[0] = 1f
        rotationMatrix[5] = 1f
        rotationMatrix[10] = 1f
        rotationMatrix[15] = 1f

        if (objFileName == null) {
            cube = Cube()
        } else {
            val reader = OBJFileReader(objFileName)
            var data: GeometryData? = reader.nextObject
            do {
                cube = Cube(data)
                //LOG.info("count coords=${data?.coordinates?.size}, indexes: ${data?.indexesCount}, vertexes: ${data?.vertexCount}")
                //LOG.info("count normla indexes=${data?.normalIndexes?.size}, normals: ${data?.normals}, vertexes: ${data?.vertexCount}")
                data = reader.nextObject
            } while (data != null)
            reader.close()
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        gl?.glClear(GL10.GL_COLOR_BUFFER_BIT)

        gl?.glMatrixMode(GL10.GL_MODELVIEW)
        gl?.glLoadIdentity()
        gl?.glTranslatef(0f, 0f, -100f)
        gl?.glMultMatrixf(rotationMatrix, 0)

        gl?.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        gl?.glEnableClientState(GL10.GL_COLOR_ARRAY)

        cube.draw(gl)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        gl?.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        LOG.info("aspect ratio: $ratio")
        gl?.glMatrixMode(GL10.GL_PROJECTION)
        gl?.glLoadIdentity()
        if (cube.mGeometryData == null) {
            gl?.glFrustumf(-ratio, ratio, -1f, 1f, 1f, 10f)
        } else {
            val modelRadius = 40f
            val zNear = 40f
            val zFar = 200f
            gl?.glFrustumf(-ratio * modelRadius, ratio * modelRadius, -modelRadius, modelRadius, zNear, zFar)
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        gl?.glDisable(GL10.GL_DITHER)
        gl?.glClearColor(1f, 1f, 1f, 1f)
    }

    class Cube {
        val vertices = floatArrayOf(
            -0.1f, -0.1f, -1f,   0.1f, -0.1f, -1f,
            0.1f,  0.1f, -1f,  -0.1f,  0.1f, -1f,
            -0.1f, -0.1f,  1f,   0.1f, -0.1f,  1f,
            0.1f,  0.1f,  1f,  -0.1f,  0.1f,  1f
        )
        val colors = floatArrayOf(
            0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f,
            0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f,
            1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f,
            1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f
        )
        val indices = byteArrayOf(
            0, 4, 5,    0, 5, 1,
            1, 5, 6,    1, 6, 2,
            2, 6, 7,    2, 7, 3,
            3, 7, 4,    3, 4, 0,
            4, 7, 6,    4, 6, 5,
            3, 0, 1,    3, 1, 2
        )
        var mVertexBuffer: FloatBuffer
        var mColorBuffer: FloatBuffer
        var mIndexBuffer: ByteBuffer
        var mGeometryData: GeometryData? = null

        init {
            val vbb = ByteBuffer.allocateDirect(vertices.size*4)
            vbb.order(ByteOrder.nativeOrder())
            mVertexBuffer = vbb.asFloatBuffer()
            mVertexBuffer.put(vertices)
            mVertexBuffer.position(0)

            val cbb = ByteBuffer.allocateDirect(colors.size*4)
            cbb.order(ByteOrder.nativeOrder())
            mColorBuffer = cbb.asFloatBuffer()
            mColorBuffer.put(colors)
            mColorBuffer.position(0)

            mIndexBuffer = ByteBuffer.allocateDirect(indices.size)
            mIndexBuffer.put(indices)
            mIndexBuffer.position(0)

            mGeometryData = null
        }

        constructor()

        constructor(geometryData: GeometryData?) : this() {
            mGeometryData = geometryData

            val vbb = ByteBuffer.allocateDirect(mGeometryData?.vertexCount!!*3*4)
            vbb.order(ByteOrder.nativeOrder())
            mVertexBuffer = vbb.asFloatBuffer()
            mVertexBuffer.put(mGeometryData?.coordinates)
            mVertexBuffer.position(0)

            val cbb = ByteBuffer.allocateDirect(mGeometryData?.vertexCount!!*4*4)
            cbb.order(ByteOrder.nativeOrder())
            mColorBuffer = cbb.asFloatBuffer()
            for (i in 0 until mGeometryData?.vertexCount!!) {
                mColorBuffer.put(1f)
                mColorBuffer.put(0f)
                mColorBuffer.put(0f)
                mColorBuffer.put(1f)
            }
            mColorBuffer.position(0)

            val indexes = mGeometryData?.indexes!!
            val indexesSize = mGeometryData?.indexesCount!!
            val ibb = ByteArray(indexesSize)
            mIndexBuffer = ByteBuffer.allocateDirect(indexesSize)
            var ibbIndex = 0
            for (i in mGeometryData?.indexes!!.indices) {
                if ((i + 1) % 4 != 0) {
                    ibb[ibbIndex++] = indexes[i].toByte()
                }
            }
            mIndexBuffer.put(ibb)
            mIndexBuffer.position(0)
        }

        fun draw(gl: GL10?) {
            gl?.glEnable(GL10.GL_CULL_FACE)
            gl?.glFrontFace(GL10.GL_CW)
            gl?.glShadeModel(GL10.GL_SMOOTH)
            gl?.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer)
            gl?.glColorPointer(4, GL10.GL_FLOAT, 0, mColorBuffer)
            if (mGeometryData != null) {
                gl?.glDrawElements(GL10.GL_TRIANGLES, mGeometryData?.indexesCount!!, GL10.GL_UNSIGNED_BYTE, mIndexBuffer)
            } else {
                gl?.glDrawElements(GL10.GL_TRIANGLES, 36, GL10.GL_UNSIGNED_BYTE, mIndexBuffer)
            }
        }
    }
}