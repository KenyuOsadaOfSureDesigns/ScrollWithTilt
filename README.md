# ScrollWithTilt
view scrolling service with sample activity. if the device detects tilt, then scroll view to tilted direction. 
this app doesn't work in standalone, and need some idea.

## Overview
when the device detect tilt by using gravity sensor, create and dispatch motion event to onTouchListener.
path of the pointer is determined by values of sensor events. 

if the divice is __shaked__ (this means, the device detects tilt motion of which angular velocity is sufficiently large), then scrolling velocity is boosted.

acceleration of the pointer equals sum of component of gravitational acceleration on the reference plane (i.e. _sliding_ acceleration on the plane) and viscous acceleration. 

> acceleration = (some coefficient) * (gravitation - (gravitation, plane direction) * (plane direction)) - (viscosity coefficient) * velocity. 

> (where (_u_, _v_) is _dot_ product of vector _u_ and _v_.)

coefficient of gravitation term is determined by the friction coefficient (hence, depends on model of friction).
