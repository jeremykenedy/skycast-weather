package com.jeremykenedy.skycastweather;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

final class SkyRenderer implements GLSurfaceView.Renderer {
  private final FloatBuffer quad =
      ByteBuffer.allocateDirect(32)
          .order(ByteOrder.nativeOrder())
          .asFloatBuffer()
          .put(new float[] {-1, -1, 1, -1, -1, 1, 1, 1});
  volatile float elapsed, cloudCover = .5f, daylight = 1, rain;
  private int program, position, time, aspect, cover, sun, wet;
  private float ratio = 16f / 9;

  private static final String VERTEX =
      "attribute vec2 p; varying vec2 uv; void main(){uv=p;gl_Position=vec4(p,0.,1.);}";
  private static final String FRAGMENT =
      "precision highp float; varying vec2 uv; uniform float t,aspect,cover,sun,wet;float hash(vec3"
          + " p){p=fract(p*.3183099+vec3(.1,.2,.3));p*=17.;return"
          + " fract(p.x*p.y*p.z*(p.x+p.y+p.z));}float noise(vec3 p){vec3"
          + " i=floor(p),f=fract(p);f=f*f*(3.-2.*f);return"
          + " mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),"
          + "mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);}float"
          + " density(vec3 p){p.x+=t*.035;float"
          + " n=noise(p*.43)*.58+noise(p*.91)*.28+noise(p*1.9)*.14;float"
          + " band=smoothstep(2.1,3.1,p.y)*(1.-smoothstep(5.5,7.2,p.y));return"
          + " max(0.,n-(.76-cover*.42))*band*1.8;}void main(){vec3"
          + " rd=normalize(vec3(uv.x*aspect,uv.y*.8+.25,1.65));vec3"
          + " light=normalize(vec3(-.6,.55,1.2));float glow=pow(max(dot(rd,light),0.),80.);float"
          + " horizon=pow(1.-max(rd.y,0.),3.);vec3"
          + " day=mix(vec3(.16,.39,.67),vec3(.68,.79,.87),horizon);vec3"
          + " night=mix(vec3(.008,.019,.052),vec3(.045,.075,.12),horizon);vec3"
          + " sky=mix(night,day,sun)+mix(vec3(.12,.16,.24),vec3(1.,.8,.51),sun)*glow*.65;sky=mix(sky,vec3(.21,.28,.35),wet*.45*sun);vec3"
          + " col=vec3(0.);float trans=1.;float start=max(0.,(2.15-1.0)/max(rd.y,.025));float"
          + " stepSize=.65;float jitter=hash(vec3(gl_FragCoord.xy,0.));for(int i=0;i<22;i++){float"
          + " dist=start+(float(i)+jitter)*stepSize;vec3 p=vec3(0.,1.,0.)+rd*dist;float"
          + " d=density(p);float shadow=exp(-density(p+light*.9)*1.8);float"
          + " alpha=1.-exp(-d*stepSize*1.8);vec3"
          + " lit=mix(vec3(.18,.23,.31),vec3(.96,.96,.92),shadow);"
          + "lit=mix(lit*.22,lit,sun);lit=mix(lit,lit*.68,wet);"
          + "col+=trans*alpha*lit;trans*=1.-alpha;if(trans<.015)break;}vec3"
          + " result=col+sky*trans;result=pow(max(result,vec3(0.)),vec3(.92));"
          + "gl_FragColor=vec4(result,1.);}";

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    program = GLES20.glCreateProgram();
    GLES20.glAttachShader(program, compile(GLES20.GL_VERTEX_SHADER, VERTEX));
    GLES20.glAttachShader(program, compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT));
    GLES20.glLinkProgram(program);
    int[] ok = new int[1];
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, ok, 0);
    if (ok[0] == 0) throw new IllegalStateException(GLES20.glGetProgramInfoLog(program));
    position = GLES20.glGetAttribLocation(program, "p");
    time = GLES20.glGetUniformLocation(program, "t");
    aspect = GLES20.glGetUniformLocation(program, "aspect");
    cover = GLES20.glGetUniformLocation(program, "cover");
    sun = GLES20.glGetUniformLocation(program, "sun");
    wet = GLES20.glGetUniformLocation(program, "wet");
  }

  private static int compile(int type, String source) {
    int shader = GLES20.glCreateShader(type);
    GLES20.glShaderSource(shader, source);
    GLES20.glCompileShader(shader);
    int[] ok = new int[1];
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, ok, 0);
    if (ok[0] == 0) throw new IllegalStateException(GLES20.glGetShaderInfoLog(shader));
    return shader;
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    GLES20.glViewport(0, 0, width, height);
    ratio = width / (float) Math.max(1, height);
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    GLES20.glUseProgram(program);
    GLES20.glUniform1f(time, elapsed);
    GLES20.glUniform1f(aspect, ratio);
    GLES20.glUniform1f(cover, cloudCover);
    GLES20.glUniform1f(sun, daylight);
    GLES20.glUniform1f(wet, rain);
    quad.position(0);
    GLES20.glEnableVertexAttribArray(position);
    GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 0, quad);
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
  }
}
