uniform mat4 uMVPMatrix;
uniform mat4 uSTMatrix;
attribute vec4 aPosition;
attribute vec4 aTextureCoord;
attribute vec4 aOverlayCoord;
varying vec2 vTextureCoord;
varying vec2 vOverlayCoord;

void main() {
    gl_Position = uMVPMatrix * aPosition;
    vTextureCoord = (uSTMatrix * aTextureCoord).xy;
    vOverlayCoord = (uSTMatrix * aOverlayCoord).xy;
}