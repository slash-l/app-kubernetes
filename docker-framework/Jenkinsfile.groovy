def SERVER_NAME = "JFrogChina-Server"

def ARTDOCKER_REGISTRY = "demo.jfrogchina.com/slash-docker-virtual"
def FRAMEWORK_REPO = "slash-docker-virtual"

node {

    def rtServer = Artifactory.server SERVER_NAME
    def rtDocker = Artifactory.docker server: rtServer
    def buildInfo = Artifactory.newBuildInfo()
    def tagName
    buildInfo.env.capture = true

    stage("Check out"){
        //Clone example project from GitHub repository
        git url: 'https://github.com/slash-l/app-kubernetes.git', branch : 'main'
    }

    //Fetch all depensencies from Artifactory
    stage('Dependencies JDK Tomcat') {
        dir('docker-framework') {
            try {
                println "Gather Java and Tomcat"

                def downloadSpec = """{
                         "files": [
                          {
                           "pattern": "slash-middleware-local/jdk/jdk-8u341-linux-x64.tar.gz",
                           "target": "jdk/jdk-8u341-linux-x64.tar.gz",
                           "flat":"true"
                          },
                          {
                           "pattern": "slash-middleware-local/tomcat/apache-tomcat-8.5.90.tar.gz",
                           "target": "tomcat/apache-tomcat-8.5.90.tar.gz",
                           "flat":"true"
                          }
                          ]
                        }"""

                rtServer.download (downloadSpec, buildInfo)
                if (fileExists('jdk/jdk-8u341-linux-x64.tar.gz') && fileExists('tomcat/apache-tomcat-8.5.90.tar.gz')) {
                    println "Downloaded dependencies"
                } else {
                    println "Missing Dependencies either jdk or tomcat - see listing below:"
                    sh 'ls -d */*'
                    throw new FileNotFoundException("Missing Dependencies")
                }
            } catch (Exception e) {
                println "Caught exception during resolution.  Message ${e.message}"
                throw e
            }
        }
    }

    //Build docker image named "docker-framework" with Java 8 and Tomcat
    stage('Build framework docker image') {
        dir ('docker-framework') {
                // sh "sed -i 's/docker.artifactory/${ARTDOCKER_REGISTRY}/' Dockerfile"
                tagName = "${ARTDOCKER_REGISTRY}/docker-framework:${env.BUILD_NUMBER}"
                println "Docker Framework Build"
                docker.build(tagName)
                println "Docker pushing -->" + tagName + " To " + FRAMEWORK_REPO
                buildInfo = rtDocker.push(tagName, FRAMEWORK_REPO, buildInfo)

                println "Docker Buildinfo"
                rtServer.publishBuildInfo buildInfo2
        }
    }

    //Test docker image
    // stage('Test') {
    //     dir('docker-framework/framework-test') {

    //         def appJarDownload = """{
    //         "files": [
    //             {
    //               "pattern": "slash-maven-dev-local/com/example/app-simple-maven/1.1.1/app-simple-maven-1.1.1.jar",
    //               "target": "app/app-simple-maven-1.1.1.jar",
    //               "props": "unit-test=pass",
    //               "flat": "true"
    //             }
    //           ]
    //         }"""

    //         rtServer.download(appJarDownload)
    //         updateDockerFile()
    //         def tagDockerFramework = "${ARTDOCKER_REGISTRY}/docker-framework-test:${env.BUILD_NUMBER}"
    //         docker.build(tagDockerFramework)
    //         if (testFramework(tagDockerFramework)) {
    //             println "Setting property and promotion"
    //             updateProperty ("functional-test=pass")
    //             sh "docker rmi ${tagName}"
    //         } else {
    //             updateProperty ("functional-test=fail; failed-test=page-not-loaded")
    //             currentBuild.result = 'UNSTABLE'
    //             sh "docker rmi ${tagName}"
    //             return
    //         }
    //     }
    // }
    // //Scan build's Artifacts in Xray
    // stage('Xray Scan') {
    //     if (XRAY_SCAN == "YES") {
    //         def xrayConfig = [
    //             'buildName'     : env.JOB_NAME,
    //             'buildNumber'   : env.BUILD_NUMBER,
    //             'failBuild'     : false
    //         ]
    //         def xrayResults = rtServer.xrayScan xrayConfig
    //         echo xrayResults as String
    //     } else {
    //         println "No Xray scan performed. To enable set XRAY_SCAN = YES"
    //     }
    //     sleep 60
    // }

    // //Promote image from local staging repositoy to production repository
    // stage ('Promote') {
    //     dir ('docker-framework') {
    //         def promotionConfig = [
    //           'buildName'          : env.JOB_NAME,
    //           'buildNumber'        : env.BUILD_NUMBER,
    //           'targetRepo'         : PROMOTE_REPO,
    //           'comment'            : 'Framework test with latest version of application',
    //           'sourceRepo'         : SOURCE_REPO,
    //           'status'             : 'Released',
    //           'includeDependencies': false,
    //           'copy'               : true
    //         ]
    //         rtServer.promote promotionConfig
    //         reTagLatest (SOURCE_REPO)
    //         reTagLatest (PROMOTE_REPO)
    //      }
    // }
}

// def updateDockerFile () {
//     def BUILD_NUMBER = env.BUILD_NUMBER
//     // sh "sed -i 's/docker.artifactory/${ARTDOCKER_REGISTRY}/' Dockerfile"
//     sh 'sed -i "s/docker-framework:latest/docker-framework:$BUILD_NUMBER/" Dockerfile'
// }

// def reTagLatest (targetRepo) {
//      def BUILD_NUMBER = env.BUILD_NUMBER
//      sh 'sed -E "s/@/$BUILD_NUMBER/" retag.json > retag_out.json'
//      switch (targetRepo) {
//           case PROMOTE_REPO :
//               sh 'sed -E "s/TARGETREPO/${PROMOTE_REPO}/" retag_out.json > retaga_out.json'
//               break
//           case SOURCE_REPO :
//                sh 'sed -E "s/TARGETREPO/${SOURCE_REPO}/" retag_out.json > retaga_out.json'
//                break
//       }
//       sh 'cat retaga_out.json'
//       withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: CREDENTIALS, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
//           def curlString = "curl -u " + env.USERNAME + ":" + env.PASSWORD + " " + SERVER_URL
//           def regTagStr = curlString +  "/api/docker/${SOURCE_REPO}/v2/promote -X POST -H 'Content-Type: application/json' -T retaga_out.json"
//           println "Curl String is " + regTagStr
//           sh regTagStr
//       }
// }

// // Following test will work only if you have jenkins running outside kubernetes.
// //test docker image by runnning container
// def testFramework (tag) {
//     def result = true
//     docker.image(tag).withRun('-p 8181:8181') {c ->
//         sleep 10
//         //def stdout = sh(script: 'curl "http://localhost:8181/swampup/"', returnStdout: true)
//         //if (stdout.contains("Welcome Docker Lifecycle Training")) {
//             // println "*** Passed Test: " + stdout
//         //} else {
//          //   println "*** Failed Test: " + stdout
//         //    result = false
//         //}
//     }
//     sh "docker rmi ${tag}"
//     return true
// }

// def updateProperty (property) {
//     withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: CREDENTIALS, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
//             def curlString = "curl -u " + env.USERNAME + ":" + env.PASSWORD + " " + "-X PUT " + SERVER_URL
//             def updatePropStr = curlString +  "/api/storage/${SOURCE_REPO}/docker-framework/${env.BUILD_NUMBER}?properties=${property}"
//             println "Curl String is " + updatePropStr
//             sh updatePropStr
//      }
// }
