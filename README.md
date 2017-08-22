# blelibrary
蓝牙ble连接以及数据传输
# 引用方式
## gradle

Add it in your root build.gradle at the end of repositories:

Step 1.

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  
Step 2. Add the dependency

	dependencies {
	        compile 'com.github.krubo:blelibrary:v1.0'
	}
  
## maven

Step 1.

 	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
 
Step 2. Add the dependency

	<dependency>
	    <groupId>com.github.krubo</groupId>
	    <artifactId>blelibrary</artifactId>
	    <version>v1.0</version>
	</dependency>
