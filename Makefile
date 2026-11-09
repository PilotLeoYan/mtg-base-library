GRADLEW ?= ./gradlew
GRADLE_FLAGS ?=

.PHONY: build test clean fabric forge neoforge

build:
	$(GRADLEW) $(GRADLE_FLAGS) build

test:
	$(GRADLEW) $(GRADLE_FLAGS) test

clean:
	$(GRADLEW) $(GRADLE_FLAGS) clean

fabric forge neoforge:
	$(GRADLEW) $(GRADLE_FLAGS) :$@:build
