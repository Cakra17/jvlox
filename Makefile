build:
	javac -d bin/ src/*.java

run:
	java -cp bin/ src.Jvlox

clean:
	rm -rf bin/
