# CI
This project is an implementation of a small continuous integration server for Java projects built with `gradle`, working with webhooks. It supports compilation, testing and notification of results using commit status. It also features a history of the past builds, which persists even if the server is reloaded.

## Run instructions
The server can be run locally, but it must be accessible from the internet. We can use `ngrok` for that, which is an utility for tunneling incoming requests from an accessible domain to our machine. 

### Download `ngrok`

#### macOS
###### Using homebrew
```shell=
brew install ngrok/ngrok/ngrok
```
###### Using zip archive
Download the zip on `ngrok`'s website, and then run the following command.

```shell=
 sudo unzip ~/Downloads/ngrok-v3-stable-darwin-amd64.zip -d /usr/local/bin
```

#### Windows
###### Using chocolatey
```shell=
choco install ngrok
```

#### Linux
###### Using apt
```shell=
curl -s https://ngrok-agent.s3.amazonaws.com/ngrok.asc | sudo tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null 
echo "deb https://ngrok-agent.s3.amazonaws.com buster main" | sudo tee /etc/apt/sources.list.d/ngrok.list
sudo apt update 
sudo apt install ngrok
```
###### Using snap
```shell=
 snap install ngrok
```

Then, sign up on `ngrok`'s website and retrieve your auth token.
```shell=
ngrok config add-authtoken <token>
```

### Run the server
The project uses `gradle` for building and dependency management. All commands below use the wrapper which is provided with the project. For Windows, use `gradle.bat` and for Linux/macOS, use `./gradlew`.

First, build the project. 

```shell=
./gradlew build
```


Then, use the `run` task to launch the server. 
```shell=
./gradlew run
```

### Set up traffic tunelling 
The `se.kth.ci.Main` class instantiates a new CI server, specifying a port number, a webhook endpoint and a working directory to use. Use this port number in the following command. It is more convenient to use the same domain each time, which avoids configuring webhooks each time we reboot the server. You can retrieve your static domain on `ngrok`'s website. 
```shell=
ngrok http <port> --domain=<domain>
```

### Configure webhook
On the repository you want to use the CI server with, configure an HTTP webhook, which sends its payload as `JSON`, to the domain you mentioned previously. 

You're done ! Upon reception of each webhook, the CI server will clone the repository on the branch on which changes were made and will trigger a build process, run all tests and sends the result as a commit status notification. 

If the repository is private, the machine which hosts the server must have the corresponding authorizations. 

### Test the project
If you want to run all unit tests, then use the following command.
```shell=
./gradlew test
```

Every method of the `se.kth.CI.CIServer` class is unit-tested. The method `parseResponse()` retreives information that is needed to clone and build a repository such as the URL and branch name. It is tested to ensure that it cannot parse non-JSON requests and that it has the correct behavior. The method `cloneRepository()` is implemented by excuting a Github repository cloning command from the program. It is tested using a test repository hosted by Rickard Cornell. The test ensures that the method only clones repositories with a valid URL and a valid branch name. For core feature n°1, the method `triggerBuild()` is also implemented by executing a build command. It is tested using one positive test and one negative test, which both use simplistic `gradle` projects in the test resources. For core feature n°2, the method `triggerTesting()` is implemented by executing the gradle test command shown abov. It is tested using one positive and one negative tests, making use of simplisic `gradle` projects in test resources as well. There also is a test for a repo that doesn't contain any test sources.  

 dis

### Generate documentation
If you want to generate documentation as HTML, then use the following command. 
```shell!
./gradlew javadoc
```
This will generate all needed components in the `/build/docs` subdirectory of your working directory. Open `index.html` in the browser of your choice to navigate through the docs. 

### Access build logs
#### Display all build history
In order to display the whole build history, follow the next steps.
1. Open up your web browser
2. Visit `<your_ngrok_domain>/builds`

#### Display the build log of a specific commit
In order to display an individual build log, follow the next steps.
+ Click the corresponding commit ID from the build history \
**OR**
+ Visit `<your_ngrok_domain>/builds/<commitID>`

## Statement of contributions
### Rickard Cornell
* Worked on Core Feature #1 - compilation with a focus on testing
* Participated in README
* Implemented the P+ assignment

### Jean Perbet
+ Set up the HTTP server using Spark.
+ Worked on Core Feature #1 - compilation.
+ Participated in README.
+ Set up a secondary CI pipeline w/ GitHub Actions.

### Raahitya Botta
+ Worked on Core Feature #2 - trigger automatic testing.
+ Participated in README.


### Zaina Ramadan
+ Worked on Core Feature #2 - trigger automatic testing 
+ Worked on the P+ feature - server keeps build history
+ Helped solve issue with Core Feature #3 - updating commit status in Github
+ Participated in README

### Elissa Arias Sosa
+ Worked on core feature #3 - notification

## Assessment about our team
Based on the checklist, we are presently in **Adjourned state** since the project has come to its conclusion. This state marks a significant achievement for us as we successfully navigated through previous stages from **Seeding** to **Performing** and have implemented the CI server. The first state of **Seeding** included identifying our goals. The rules had naming conventions, and issues had already been established during the previous assignment. Then came the important states of **Collaborating** and **Performing**, where we worked on our assigned issues and regularly committed to the branches. We transitionned out of the **Performing** state after meeting up with required goals and are now focused on documenting and reflecting on outcomes. There were several obstacles at the beginning since there were several aspects of the project that we were new to. For example, we had to use different tools that we were completely new and unfamiliar with such as `gradle` for build & test automation. Initially, `maven` was suggested by a TA but later after reviewing it, we found `gradle` to be more advanced and easier to use. `sqlite` was also new to us.  However, we learned to use them throughout the process and now feel quite comfortable with them.
