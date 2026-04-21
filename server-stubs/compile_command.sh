# from claude, this was what I needed to run to compile properly
javac -encoding UTF-8 -d out \
  server-stubs/gl/shaders/Texture.java \
  server-stubs/android/util/Log.java \
  app/src/main/java/com/ex/bwb/cards/Card.java \
  app/src/main/java/com/ex/bwb/cards/CardType.java \
  app/src/main/java/com/ex/bwb/cards/CardEffect.java \
  app/src/main/java/com/ex/bwb/cards/Action.java \
  app/src/main/java/com/ex/bwb/cards/Attack.java \
  app/src/main/java/com/ex/bwb/cards/BigBuddy.java \
  app/src/main/java/com/ex/bwb/cards/Signature.java \
  app/src/main/java/com/ex/bwb/cards/LilBuddy.java \
  app/src/main/java/com/ex/bwb/cards/ShakeUp.java \
  app/src/main/java/com/ex/bwb/cards/Objective.java \
  app/src/main/java/com/ex/bwb/cards/Effects.java \
  app/src/main/java/com/ex/bwb/game/*.java \
  app/src/main/java/com/ex/bwb/Player.java \
  app/src/main/java/com/ex/bwb/networking/packets/*.java \
  app/src/main/java/com/ex/bwb/networking/StandaloneServer.java