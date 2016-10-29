# Introduction

Tasks can be implemented in any public method of a `Serializable` class. Per default a method `public void run()`
is used to execute a task, therefore it has to implement interface `java.lang.Runnable`.

Example:

```
Fork<MyTask, Void> f = new Fork<MyTask, Void>(new MyTask());
	
f.execute();
	
f.waitFor();
	
...
	
class MyTask implements Serializable, Runnable {

	public void run() {
		...
	}
}
```

The fork process ends when the `MyTask` method `run` finishes.

## Description

Term _fork_ is borrowed from fork processes on Unix systems. In Java 6 are Unix like forks not possible. The upcoming release of Java 7 (end of year 2010) will support a Fork/Join mechanism, which seems to come very close to real forks, but also with some restrictions, see below Java 7 Fork/Join Extension. The intention of this package is to provide something similar by using Java sub processes. The created process that executes a task in a Java `Fork` inherits the environment of the parent VM process per default, including class path and system properties. The API gives as much as possible freedom in implementing the task that should be executed in the subprocess, furthermore the final state of the task object is propagated back to the parent process. 

Conditions for task execution:

 * Task object implements interface `Serializable`.
 * Task object implements a public startup method. If this method has a return value and/or arguments they need also to implement interface `Serializable`.
 * Permission to use Java Reflection.
 * Permission to create, read and write temporary files.
 * Permission to establish socket connections and listeners.

Object Serialization is needed to pass a task object to the internal `ForkRunner`
that implements the main method to launch a fork. `ForkRunner` invokes the chosen task method via Java Reflection. Vice versa the task object, 
its state and an optional return value of the task method are propagated back 
to the parent process using Object Serialization when the task method returns.

Reflection is used to give flexibility in choosing the task method 
to be executed in the fork subprocess. As a user of the API you have to identify
the task method as object of type `java.lang.reflect.Method`, see also Reflection packages.  
Alternatively `public void run()` of a `Runnable` task object
is used per default.

## Task Method with Return Value

The following sample shows a fork running task method `encodeText` with one `String` argument
and a `String` return value:

```
Fork<MyTask, String> f = 
	new Fork<MyTask, String>(
		new MyTask(), 
		MyTask.class.getMethod("encodeText", String.class), 
		"some text to be converted to some encoding..."
		);

f.execute();
	
f.waitFor();
	
String result = f.getReturnValue();
	
...
	
class MyTaks implements Serializable {
		
	public String encodeText(String text) {
		String encodedText;
		...
		return encodedText;
	}
}
```

For more capabilities, e.g., callable tasks and usage scenarios inspect javadoc (see gforc-javadoc.jar) and the JUnit tests (see gfork-tests.jar). A sample application shows how a Swing `JFrame` can be run in a fork, therefore see  `org.gfork.swing` in gfork.jar.

## Java 7 Fork/Join Extension (JSR 166) 

  * a lightweight task framework to split problems into many tasks to take advantage of dozens to hundreds of local processors/cores 
  * more efficient than threads, at the price of some usage limitations, e.g., operating on purely isolated objects… (see Java 7 doc of `ForkJoinTask`) 
  * to automate parallel aggregate operations on collections of elements (see Java 7 doc of `RecursiveAction`, `RecursiveTask`) 

Java Concurrency Wiki, https://en.wikipedia.org/wiki/Java_concurrency
<br>Doug Lea, A Java Fork/Join Framework, http://gee.cs.oswego.edu/dl/papers/fj.pdf
