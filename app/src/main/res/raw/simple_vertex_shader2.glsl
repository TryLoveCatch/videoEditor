uniform mat4 u_Matrix;
attribute vec4 a_Position;
attribute vec2 a_Coordinate;

varying vec2 v_Coordinate;

void main()
{
  v_Coordinate = a_Coordinate;
  gl_Position = u_Matrix * a_Position;
}