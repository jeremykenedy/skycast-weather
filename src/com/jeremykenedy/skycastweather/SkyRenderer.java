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
  private int program, position, time, aspect, cover, sun, wet, blend, jitter;
  private int blitProgram, blitPosition, noiseTexture;
  private final int[] historyTextures = new int[2], historyBuffers = new int[2];
  private final SkyHistory history = new SkyHistory();
  private int surfaceWidth, surfaceHeight;
  private float ratio = 16f / 9;

  private static final String VERTEX =
      "attribute vec2 p; varying highp vec2 uv; void main(){uv=p;gl_Position=vec4(p,0.,1.);}";
  private static final String FRAGMENT =
      "precision highp float; varying highp vec2 uv; uniform float"
          + " t,aspect,cover,sun,wet,historyWeight,phase; uniform sampler2D"
          + " volume,previousFrame;float noise(vec3 p){vec3"
          + " i=floor(p),f=fract(p);f=f*f*(3.-2.*f);float z=mod(i.z,32.);vec2"
          + " tile=vec2(mod(z,8.),floor(z/8.));vec2"
          + " coord=(mod(i.xy+f.xy,32.)+1.5+tile*34.)/vec2(272.,136.);vec4"
          + " tex=texture2D(volume,coord);return mix(tex.r,tex.a,f.z);}float density(vec3"
          + " p){p.x+=t*.065;p.z+=t*.012;float"
          + " n=noise(p*.48+vec3(t*.003,0.,0.))*.46+noise(p*.98-vec3(0.,0.,t*.007))*.28"
          + "+noise(p*2.1+vec3(0.,t*.012,0.))*.17+noise(p*4.4)*.09;float"
          + " band=smoothstep(2.1,2.9,p.y)*(1.-smoothstep(5.6,7.2,p.y));return"
          + " smoothstep(.76-cover*.42,.95-cover*.42,n)*band*1.4;}void main(){vec3"
          + " rd=normalize(vec3(uv.x*aspect,uv.y*.72+.9,1.65));vec3"
          + " light=normalize(vec3(-.6,.55,1.2));float glow=pow(max(dot(rd,light),0.),80.);float"
          + " horizon=pow(1.-max(rd.y,0.),3.);vec3"
          + " day=mix(vec3(.16,.39,.67),vec3(.68,.79,.87),horizon);vec3"
          + " night=mix(vec3(.008,.019,.052),vec3(.045,.075,.12),horizon);vec3"
          + " sky=mix(night,day,sun)+mix(vec3(.12,.16,.24),vec3(1.,.8,.51),sun)*glow*.65;sky=mix(sky,vec3(.21,.28,.35),wet*.45*sun);vec3"
          + " col=vec3(0.);float trans=1.;float start=(2.15-1.0)/rd.y;float stepSize=.5;float"
          + " offset=fract(sin(dot(gl_FragCoord.xy,vec2(12.9898,78.233)))*43758.5453+phase*.618033989);for(int"
          + " i=0;i<28;i++){float dist=start+(float(i)+offset)*stepSize;vec3"
          + " p=vec3(0.,1.,0.)+rd*dist;float d=density(p);float"
          + " shadow=exp(-density(p+light*.9)*2.4);float alpha=1.-exp(-d*stepSize*1.8);vec3"
          + " lit=mix(vec3(.18,.23,.31),vec3(.98,.97,.93),shadow);"
          + "lit=mix(lit*vec3(.26,.33,.46),lit,sun);lit=mix(lit,lit*.68,wet);"
          + "col+=trans*alpha*lit;trans*=1.-alpha;if(trans<.015)break;}vec3"
          + " result=col+sky*trans;result=pow(max(result,vec3(0.)),vec3(.92));gl_FragColor=vec4(mix(result,texture2D(previousFrame,uv*.5+.5).rgb,historyWeight),1.);}";

  private static final String BLIT =
      "precision mediump float;varying highp vec2 uv;uniform sampler2D image;void"
          + " main(){gl_FragColor=texture2D(image,uv*.5+.5);}";

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    java.util.Arrays.fill(historyTextures, 0);
    java.util.Arrays.fill(historyBuffers, 0);
    history.reset();
    program = link(FRAGMENT);
    blitProgram = link(BLIT);
    blitPosition = GLES20.glGetAttribLocation(blitProgram, "p");
    int[] textures = new int[1];
    GLES20.glGenTextures(1, textures, 0);
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    noiseTexture = textures[0];
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, noiseTexture);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    byte[] atlas = SkyNoise.atlas();
    ByteBuffer bytes = ByteBuffer.allocateDirect(atlas.length).put(atlas);
    bytes.position(0);
    GLES20.glTexImage2D(
        GLES20.GL_TEXTURE_2D,
        0,
        GLES20.GL_LUMINANCE_ALPHA,
        SkyNoise.WIDTH,
        SkyNoise.HEIGHT,
        0,
        GLES20.GL_LUMINANCE_ALPHA,
        GLES20.GL_UNSIGNED_BYTE,
        bytes);
    GLES20.glUseProgram(program);
    GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "volume"), 0);
    GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "previousFrame"), 1);
    blend = GLES20.glGetUniformLocation(program, "historyWeight");
    jitter = GLES20.glGetUniformLocation(program, "phase");
    GLES20.glUseProgram(blitProgram);
    GLES20.glUniform1i(GLES20.glGetUniformLocation(blitProgram, "image"), 1);
    position = GLES20.glGetAttribLocation(program, "p");
    time = GLES20.glGetUniformLocation(program, "t");
    aspect = GLES20.glGetUniformLocation(program, "aspect");
    cover = GLES20.glGetUniformLocation(program, "cover");
    sun = GLES20.glGetUniformLocation(program, "sun");
    wet = GLES20.glGetUniformLocation(program, "wet");
  }

  private static int link(String fragmentSource) {
    int linked = GLES20.glCreateProgram();
    int vertex = compile(GLES20.GL_VERTEX_SHADER, VERTEX);
    int fragment = compile(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
    GLES20.glAttachShader(linked, vertex);
    GLES20.glAttachShader(linked, fragment);
    GLES20.glLinkProgram(linked);
    GLES20.glDeleteShader(vertex);
    GLES20.glDeleteShader(fragment);
    int[] ok = new int[1];
    GLES20.glGetProgramiv(linked, GLES20.GL_LINK_STATUS, ok, 0);
    if (ok[0] == 0) throw new IllegalStateException(GLES20.glGetProgramInfoLog(linked));
    return linked;
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
    surfaceWidth = width;
    surfaceHeight = height;
    ratio = width / (float) Math.max(1, height);
    GLES20.glDeleteTextures(2, historyTextures, 0);
    GLES20.glDeleteFramebuffers(2, historyBuffers, 0);
    GLES20.glGenTextures(2, historyTextures, 0);
    GLES20.glGenFramebuffers(2, historyBuffers, 0);
    GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
    for (int i = 0; i < 2; i++) {
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, historyTextures[i]);
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
      GLES20.glTexParameteri(
          GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
      GLES20.glTexParameteri(
          GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
      GLES20.glTexImage2D(
          GLES20.GL_TEXTURE_2D,
          0,
          GLES20.GL_RGBA,
          width,
          height,
          0,
          GLES20.GL_RGBA,
          GLES20.GL_UNSIGNED_BYTE,
          null);
      GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, historyBuffers[i]);
      GLES20.glFramebufferTexture2D(
          GLES20.GL_FRAMEBUFFER,
          GLES20.GL_COLOR_ATTACHMENT0,
          GLES20.GL_TEXTURE_2D,
          historyTextures[i],
          0);
      if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE)
        throw new IllegalStateException("Cloud history surface unavailable");
      GLES20.glClearColor(0, 0, 0, 1);
      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    }
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    history.reset();
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    float weight = history.blend(daylight, cloudCover, rain);
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, historyBuffers[history.writeIndex()]);
    GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, noiseTexture);
    GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, historyTextures[history.readIndex()]);
    GLES20.glUseProgram(program);
    GLES20.glUniform1f(blend, weight);
    GLES20.glUniform1f(jitter, history.phase());
    GLES20.glUniform1f(time, elapsed);
    GLES20.glUniform1f(aspect, ratio);
    GLES20.glUniform1f(cover, cloudCover);
    GLES20.glUniform1f(sun, daylight);
    GLES20.glUniform1f(wet, rain);
    drawQuad(position);
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    GLES20.glUseProgram(blitProgram);
    GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, historyTextures[history.writeIndex()]);
    drawQuad(blitPosition);
    history.advance();
  }

  private void drawQuad(int attribute) {
    quad.position(0);
    GLES20.glEnableVertexAttribArray(attribute);
    GLES20.glVertexAttribPointer(attribute, 2, GLES20.GL_FLOAT, false, 0, quad);
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
  }
}
