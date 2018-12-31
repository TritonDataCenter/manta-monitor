## Running the project using Intellij
The following steps describe how to run the manta-monitor project using Intellij as the IDE.

NOTE: The Intellij Community Edition is good enough to run the application.

### Import the project into Intellij
Follow the screen shots below to import the manta-monitor project into Intellij:

* Start Intellij and select Import Project
![Step 1](img/Intellij-Screen1.png?raw=true)

* Import Project from existing sources and select Maven
![](img/Intellij-Screen2.png?raw=true)

* Select the default options as presented by Intellij and hit Next

![](img/Intellij-Screen3.png?raw=true)

![](img/Intellijj-Screen4.png?raw=true)

![](img/Intellij-Screen5.png?raw=true)

![](img/Intellij-Screen6.png?raw=true)

### Installing the project from Intellij
Once you have successfully imported the project, you can use the Intellij Maven Project View to install the project.

Select the Maven projects view within Intellij (the tab Maven Projects is located on the right side vertically) and select install as shown:

(Note: If you do not see the Maven Projects tab, then, select the View tab at the top and check if Maven Projects view is enabled)
![](img/Intellij-ScreenShot7.png?raw=true)

### Running the project from Intellij
From Intellij 'Run' tab select Edit Configurations and add the configurations as shown below:
You will need the following env variables ready before adding the run configuration:
* HONEYBADGER_API_KEY
* MANTA_USER
* MANTA_KEY_ID
* JAVA_ENV = production/development
* INSTANCE_METADATA_PROPS_FILE = src/test/resources/example-instance-metadata.properties
* MANTA_METRIC_REPORTER_MODE = JMX

##### Add the run configuration in Intellij.
Select the Edit Configurations option from the Run tab
![](img/Intellij-ScreenRunTabOptions.png?raw=true)

Add a new run configuration by selecting Applications as shown below:
![](img/Intellij-ScreenRunConfig.png?raw=ture)

Add the configuration options as:
![](img/Intellij-EditConfig.png?raw=true)

Save the above configuration and now you will be able to RUN the project in Intellij