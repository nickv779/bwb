# Game Engine Manual

Below are the rough notes for using this package with the purpose of game engine shenanigans. Each
subheading below refers to an individual package and explains the contents within.

## Controllers

Controller input functionality, the first few (prepended with J4Q) are fitted for the VR
controllers while the last two are relevant for this project.

- ./J4QController: VR controller ABC used in J4QLeft and J4QRight.
- ./J4QJoystick: used for movement with joystick
- ./J4QPose: used for tracking position and orientation of the controller
- ./J4QPressureButton: tracks pressure applied to buttons on controller
- ./J4QToggleButton: tracks the state of controller buttons (i.e. pressed, released)
- ./ObjectPicker: tracks which 3D object is picked in the scene, used in TouchScreen
- ./TouchScreen: provides touch controls and events

## Formats

Model formats supported, currently only contains OBJ file support.

## Geometry

Provides orientation, position, and transform classes and methods. Available transformations are:

- Rotate (direction, xyz axis)
- Translate (direction, xyz axis)
- Reset (return to original position)

## Materials

Shader materials, only includes PhongMaterial as of now.

## Models

Contains different model implementations all extending from or using the GameObject class.

- ./Background360: a background environment a player can view in 360 degrees
- ./Component: an ABC that only holds a GameObject member variable
- ./GameObject: similar to Unity game objects, contains ways to use and manipulate components within a game object
- ./Mesh: extends Component, used like the Unity Mesh class
- ./MonoBehavior: an ABC extending Component
- ./ObjectMaker: extends geometry.Transform, creates 3D shapes like a cone and cylinder
- ./Spaceship: an example class that creates a spaceship using ObjectMaker, extends from GameObject

## Physics

Physics implementation using JBullet, a Java port of Bullet physics engine.
Centers around the PhysicsEngine class, the physics infrastructure for implementing JBullet physics.
Requires rigid bodies to be defined of course.

## Shaders

Different GLSL shaders are located here.

## Audio.java

Audio implementation.

## J4Q.java

Java for Quest class, necessary for Meta Quest 2 integration. For the purposes of this project, not
necessary. Leaving the file as is though.