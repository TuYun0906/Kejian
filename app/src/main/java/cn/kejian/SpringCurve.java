package cn.kejian;
public final class SpringCurve {
 public static float value(double stiffness,double damping,double seconds){double w=Math.sqrt(stiffness);if(damping==1)return (float)(1-Math.exp(-w*seconds)*(1+w*seconds));double wd=w*Math.sqrt(1-damping*damping);return (float)(1-Math.exp(-damping*w*seconds)*(Math.cos(wd*seconds)+damping*w/wd*Math.sin(wd*seconds)));}
}
