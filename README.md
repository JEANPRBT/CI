# CI
This  project is an implementation of a small continuous integration server for Java projects built with `gradle`, working with webhooks. It supports compilation, testing and notification of results using commit status. It also features a history of the past builds, which persists even if the server is reloaded.

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

Every method of the `se.kth.CI.CIServer` class are unit-tested. The method `parseResponse()` is tested to ensure that it cannot parse non-JSON requests and that it has the correct behavior. The method `cloneRepository()` is tested using a test repository hosted by Rickard Cornell. It ensures that it only clones repositories with a valid URL and a valid branch name. The method `triggerBuild()` is tested using one positive test and one negative test, which both use simplistic `gradle` projects in the test resources. 

### Generate documentation
If you want to generate documentation as HTML, then use the following command. 
```shell!
./gradlew javadoc
```
This will generate all needed components in the `/build/docs` subdirectory of your working directory. Open `index.html` in the browser of your choice to navigate through the docs. 


## Statement of contributions
### Rickard Cornell
* Worked on Core Feature #1 - compilation
* Participated in README
* Implemented P+

### Jean Perbet
+ Set up the HTTP server using Spark.
+ Worked on Core Feature #1 - compilation.
+ Participated in README.
+ Set up a secondary CI pipeline w/ GitHub Actions.

### Raahitya Botta

### Zaina Ramadan

### Elissa Arias Sosa
