# Execution
	java -jar s3j.jar ~/Desktop/config.xml

# Sample Configuration XML
	<?xml version="1.0" encoding="UTF-8"?>
	<config xmlns="http://hardisonbrewing.org/schemas/model" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://hardisonbrewing.org/schemas/model config.xsd">
		<bucket>bucket</bucket>
		<accessKey>accessKey</accessKey>
		<accessKeyId>accessKeyId</accessKeyId>
		<privateKey>/Users/username/.ssh/id_rsa</privateKey>
		<resources>
			<resource>
				<directory>~/Desktop</directory>
				<includes>
					<include>(.*)\.png</include>
				</includes>
			</resource>
	<!-- 		<resource> -->
	<!-- 			<directory>/</directory> -->
	<!-- 			<includes> -->
	<!-- 				<include>(.*)\.pdf</include> -->
	<!-- 			</includes> -->
	<!-- 			<excludes> -->
	<!-- 				<exclude>(.*)/Frameworks/QTKit.framework/(.*)\.pdf</exclude> -->
	<!-- 			</excludes> -->
	<!-- 		</resource> -->
		</resources>
	</config>
