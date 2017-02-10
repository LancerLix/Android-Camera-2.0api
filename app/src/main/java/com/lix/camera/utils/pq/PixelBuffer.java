/*
 * Copyright (C) 2012 CyberAgent
 * Copyright (C) 2010 jsemler 
 * 
 * Original publication without License
 * http://www.anddev.org/android-2d-3d-graphics-opengl-tutorials-f2/possible-to-do-opengl-off-screen-rendering-in-android-t13232.html#p41662
 */

package com.lix.camera.utils.pq;

import static javax.microedition.khronos.egl.EGL10.EGL_ALPHA_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_BLUE_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_DEFAULT_DISPLAY;
import static javax.microedition.khronos.egl.EGL10.EGL_DEPTH_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_GREEN_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_HEIGHT;
import static javax.microedition.khronos.egl.EGL10.EGL_NONE;
import static javax.microedition.khronos.egl.EGL10.EGL_NO_CONTEXT;
import static javax.microedition.khronos.egl.EGL10.EGL_RED_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_STENCIL_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_WIDTH;
import static javax.microedition.khronos.opengles.GL10.GL_RGBA;
import static javax.microedition.khronos.opengles.GL10.GL_UNSIGNED_BYTE;

import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.util.Log;

public class PixelBuffer {
    
    public interface PixelBufferListener {
        public void OnGLDataCopyStart();
        public void OnGLDataCopyFinish(GPUImageRenderer rd, PixelBuffer pb);
    }
    
    final static String TAG = PixelBuffer.class.getSimpleName();
    final static boolean LIST_CONFIGS = false;

    GLSurfaceView.Renderer mRenderer; // borrow this interface
    int mWidth, mHeight;
    PixelBufferListener mPixelBufferListener;

    EGL10 mEGL;
    EGLDisplay mEGLDisplay;
    EGLConfig[] mEGLConfigs;
    EGLConfig mEGLConfig;
    EGLContext mEGLContext;
    EGLSurface mEGLSurface;
    GL10 mGL;

    String mThreadOwner;

    public PixelBuffer(final int width, final int height) {
        mWidth = width;
        mHeight = height;
    }
    
    public boolean checkSupport() {
        int[] version = new int[2];
        
        // No error checking performed, minimum required code to elucidate logic
        mEGL = (EGL10) EGLContext.getEGL();
        mEGLDisplay = mEGL.eglGetDisplay(EGL_DEFAULT_DISPLAY);
        mEGL.eglInitialize(mEGLDisplay, version);
        mEGLConfig = chooseConfig();
        
        int mw = getConfigAttrib(mEGLConfig, EGL10.EGL_MAX_PBUFFER_WIDTH);
        int mh = getConfigAttrib(mEGLConfig, EGL10.EGL_MAX_PBUFFER_HEIGHT);
        
        Log.i(TAG, "EGL_MAX_PBUFFER_WIDTH :" + mw + " EGL_MAX_PBUFFER_HEIGHT:" + mh);
        
        if(mWidth > mw || mHeight > mh) {
            return false;
        }
        
        return true;
    }
    
    public void prepareGLEnv() {
        int[] version = new int[2];
        int[] attribList = new int[] {
                EGL_WIDTH, mWidth,
                EGL_HEIGHT, mHeight,
                EGL_NONE
        };

        // No error checking performed, minimum required code to elucidate logic
        mEGL = (EGL10) EGLContext.getEGL();
        mEGLDisplay = mEGL.eglGetDisplay(EGL_DEFAULT_DISPLAY);
        mEGL.eglInitialize(mEGLDisplay, version);
        mEGLConfig = chooseConfig(); // Choosing a config is a little more
                                     // complicated

        // mEGLContext = mEGL.eglCreateContext(mEGLDisplay, mEGLConfig,
        // EGL_NO_CONTEXT, null);
        int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        int[] attrib_list = {
                EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL10.EGL_NONE
        };
        mEGLContext = mEGL.eglCreateContext(mEGLDisplay, mEGLConfig, EGL_NO_CONTEXT, attrib_list);
        if (mEGLContext == EGL10.EGL_NO_CONTEXT)
        {  
            Log.d(TAG, "no CONTEXT");
        }

        mEGLSurface = mEGL.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig, attribList);
        if (mEGLSurface == EGL10.EGL_NO_SURFACE)
        {
            //mEgl.eglDestroySurface(mEglDisplay, mEglPBSurface);
            int ec = mEGL.eglGetError();
            if (ec == EGL10.EGL_BAD_DISPLAY)
            {
                Log.d(TAG, "EGL_BAD_DISPLAY");
            }
            if (ec == EGL10.EGL_BAD_DISPLAY)
            {
                Log.d(TAG, "EGL_BAD_DISPLAY");
            }
            if (ec == EGL10.EGL_NOT_INITIALIZED)
            {
                Log.d(TAG, "EGL_NOT_INITIALIZED");
            }
            if (ec == EGL10.EGL_BAD_CONFIG)
            {
                Log.d(TAG, "EGL_BAD_CONFIG");
            }
            if (ec == EGL10.EGL_BAD_ATTRIBUTE)
            {
                Log.d(TAG, "EGL_BAD_ATTRIBUTE");
            }
            if (ec == EGL10.EGL_BAD_ALLOC)
            {
                Log.d(TAG, "EGL_BAD_ALLOC");
            }
            if (ec == EGL10.EGL_BAD_MATCH)
            {
                Log.d(TAG, "EGL_BAD_MATCH");
            }
        }
        
        if (!mEGL.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
            Log.d(TAG, "bind failed ECODE:" + mEGL.eglGetError());
        }

        mGL = (GL10) mEGLContext.getGL();

        // Record thread owner of OpenGL context
        mThreadOwner = Thread.currentThread().getName();
    }

    public void setRenderer(final GLSurfaceView.Renderer renderer) {
        mRenderer = renderer;

        // Does this thread own the OpenGL context?
        if (!Thread.currentThread().getName().equals(mThreadOwner)) {
            Log.e(TAG, "setRenderer: This thread does not own the OpenGL context.");
            return;
        }

        // Call the renderer initialization routines
        mRenderer.onSurfaceCreated(mGL, mEGLConfig);
        mRenderer.onSurfaceChanged(mGL, mWidth, mHeight);
    }
    
    public void setPixelBufferListener(PixelBufferListener listener) {
        mPixelBufferListener = listener;
    }

    public Bitmap getBitmap() {
        // Do we have a renderer?
        if (mRenderer == null) {
            Log.e(TAG, "getBitmap: Renderer was not set.");
            return null;
        }

        // Does this thread own the OpenGL context?
        if (!Thread.currentThread().getName().equals(mThreadOwner)) {
            Log.e(TAG, "getBitmap: This thread does not own the OpenGL context.");
            return null;
        }

        // Call the renderer draw routine (it seems that some filters do not
        // work if this is only called once)
        mRenderer.onDrawFrame(mGL);
//        mRenderer.onDrawFrame(mGL);
        return convertToBitmap();
    }

    public void destroy() {
//        mRenderer.onDrawFrame(mGL);
//        mRenderer.onDrawFrame(mGL);
        mEGL.eglMakeCurrent(mEGLDisplay, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);

        mEGL.eglDestroySurface(mEGLDisplay, mEGLSurface);
        mEGL.eglDestroyContext(mEGLDisplay, mEGLContext);
        mEGL.eglTerminate(mEGLDisplay);
    }

    private EGLConfig chooseConfig() {
        int[] attribList = new int[] {
                EGL_DEPTH_SIZE, 0,
                EGL_STENCIL_SIZE, 0,
                EGL_RED_SIZE, 8,
                EGL_GREEN_SIZE, 8,
                EGL_BLUE_SIZE, 8,
                EGL_ALPHA_SIZE, 8,
                EGL10.EGL_RENDERABLE_TYPE, 4,
                EGL_NONE
        };

        // No error checking performed, minimum required code to elucidate logic
        // Expand on this logic to be more selective in choosing a configuration
        int[] numConfig = new int[1];
        mEGL.eglChooseConfig(mEGLDisplay, attribList, null, 0, numConfig);
        int configSize = numConfig[0];
        mEGLConfigs = new EGLConfig[configSize];
        mEGL.eglChooseConfig(mEGLDisplay, attribList, mEGLConfigs, configSize, numConfig);

        if (LIST_CONFIGS) {
            listConfig();
        }

        return mEGLConfigs[0]; // Best match is probably the first configuration
    }

    private void listConfig() {
        Log.i(TAG, "Config List {");

        for (EGLConfig config : mEGLConfigs) {
            int d, s, r, g, b, a, id, mw, mh;

            // Expand on this logic to dump other attributes
            d = getConfigAttrib(config, EGL_DEPTH_SIZE);
            s = getConfigAttrib(config, EGL_STENCIL_SIZE);
            r = getConfigAttrib(config, EGL_RED_SIZE);
            g = getConfigAttrib(config, EGL_GREEN_SIZE);
            b = getConfigAttrib(config, EGL_BLUE_SIZE);
            a = getConfigAttrib(config, EGL_ALPHA_SIZE);
            id = getConfigAttrib(config, EGL10.EGL_CONFIG_ID);
            mw = getConfigAttrib(config, EGL10.EGL_MAX_PBUFFER_WIDTH);
            mh = getConfigAttrib(config, EGL10.EGL_MAX_PBUFFER_HEIGHT);
            Log.i(TAG, "    <d,s,r,g,b,a,id,mw,mh> = <" + d + "," + s + "," +
                    r + "," + g + "," + b + "," + a +  "," + id +  "," + mw +  "," + mh +">");
        }

        Log.i(TAG, "}");
    }

    private int getConfigAttrib(final EGLConfig config, final int attribute) {
        int[] value = new int[1];
        return mEGL.eglGetConfigAttrib(mEGLDisplay, config,
                attribute, value) ? value[0] : 0;
    }

    private Bitmap convertToBitmap() {
        if(null != mPixelBufferListener) {
            mPixelBufferListener.OnGLDataCopyStart();
        }
        
        IntBuffer ib = IntBuffer.allocate(mWidth * mHeight);
        mGL.glReadPixels(0, 0, mWidth, mHeight, GL_RGBA, GL_UNSIGNED_BYTE, ib);
        
        if(null != mPixelBufferListener) {
            mPixelBufferListener.OnGLDataCopyFinish((GPUImageRenderer) mRenderer, this);
        }

        Bitmap processedBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        processedBitmap.copyPixelsFromBuffer(ib);
        
        ib = null;
        System.gc();
        
        return processedBitmap;
    }
}
