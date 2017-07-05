#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTextureCoord;
varying vec2 vOverlayCoord;
uniform samplerExternalOES sTexture;
uniform sampler2D oTexture;
uniform int overlayFlag;
/*uniforms*/

vec4 makeOverlayColor() {
    vec4 fg_color = texture2D(oTexture, vOverlayCoord);
    float colorR = (1.0 - fg_color.a) * gl_FragColor.r + fg_color.a * fg_color.r;
    float colorG = (1.0 - fg_color.a) * gl_FragColor.g + fg_color.a * fg_color.g;
    float colorB = (1.0 - fg_color.a) * gl_FragColor.b + fg_color.a * fg_color.b;
    gl_FragColor = vec4(colorR, colorG, colorB, gl_FragColor.a);
    return gl_FragColor;
}

/*begin*/
int applyFilter() {
    gl_FragColor = texture2D(sTexture, vTextureCoord);
    return 0;
}
/*end*/

void main() {

    applyFilter();


    int hasOverly = overlayFlag;  //uniform int 必须先赋给local变量，才算被使用，不会被优化调
    if (hasOverly > 0)
        makeOverlayColor();
}


