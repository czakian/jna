all: 
	javac -classpath .:jna.jar LinuxLib.java

run:
	 java -classpath .:jna.jar LinuxLib
clean:
	rm *.class