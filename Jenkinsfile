pipeline {
  environment {
     APP_NAME = 'mobile-sdk'
     BINTRAY_API_KEY = credentials('mindbox-bintray-api-key	')
     BINTRAY_USER = 'uit-devops'
     SNYK_TOKEN = 'snyk-api-token'
     SNYK_ORG = 'uit'
  }
   agent {node {label 'mobile-builder-1'}}
   stages {
        stage('Preflight check'){
            when { anyOf { branch 'release'; branch 'master'; branch 'develop'; branch 'jenkins-pipeline'} }
            post {
                success {
                    slackSend channel: 'jenkins-mindbox', \
                    teamDomain: 'umbrellaitcom', \
                    color: '#5cb589', \
                    message: "MindBox » Android-SDK - ${env.JOB_BASE_NAME} ${env.BUILD_DISPLAY_NAME} Started (<${env.RUN_DISPLAY_URL}|Open>)",
                    tokenCredentialId: 'umbrella.devops-slack-integration-token'
                }
            }
            steps {sh "env | sort"}
        }

       stage('Tests'){
            parallel {
                stage ('Gradle Lint') {
                    // when { anyOf { branch 'develop'; branch 'release'; branch 'jenkins-pipeline'} }
                    steps {
                        sh label: 'Running lint check', script: './gradlew check'
                    }
                }
                stage ('Unit Tests') {
                    // when { anyOf { branch 'release'; branch 'master'; branch 'develop'; branch 'jenkins-pipeline'} }
                    steps {
                        sh label: 'Running Unit test', script: './gradlew test'
                    }
                }
                // stage ('Vulnerabilities Tests') {
                //     when { anyOf { branch 'release'; branch 'master'; branch 'develop'; branch 'jenkins-pipeline'} }
                //     steps {
                //         script {
                //             sh label: 'Checking dependencies with Snyk api',
                //             script: 'docker run -e SNYK_TOKEN=$SNYK_TOKEN \
                //                     -e MONITOR=true \
                //                     -e USER_ID=$(id -u $USER) \
                //                     -v $PWD/:/project \
                //                     snyk/snyk-cli:gradle-5.4 test --org=$SNYK_ORG --project-name=$APP_NAME'
                //         }
                //     }
                // }
            }
        }

        stage('Build'){
            when { anyOf { branch 'release'; branch 'master'; branch 'develop'; branch 'jenkins-pipeline'} }
            steps {
                script {
                    // private credentials used here because of client specifics
                    sshagent (credentials: ['umbrella-roman-parfinenko']) {
                        sh label: 'Project cleanup', script:  './gradlew clean'
                        sh label: 'Project build', script:  './gradlew build'
                        sh label: 'Build a debug APK', script:  './gradlew assembleDebug'
                        sh label: 'Assembles Test applications', script:  './gradlew assembleAndroidTest'
                    }
                }
            }
        }
        stage('Upload'){
            when { branch pattern: "release-\\d+", comparator: "REGEXP"}
            steps {
                script {
                    sshagent (credentials: ['umbrella-roman-parfinenko']) {
                        sh label: 'Project cleanup', script:  './gradlew clean'
                        sh label: 'Project build', script:  './gradlew build'
                        sh label: 'SDK upload', script:  './gradlew build bintrayUpload -PbintrayUser=$BINTRAY_USER -PbintrayApiKey=$BINTRAY_API_KEY'
                        sh label: 'Upload to Bintray', script ' APP_VERSION=$(cat gradle.properties |grep SDK_VERSION_NAME | cut -f2 -d"=") \
                            curl -T \
                            ./mobile-sdk/build/outputs/aar/mobile-sdk-release.aar \
                            -u$BINTRAY_USER:$BINTRAY_API_KEY \
                            https://api.bintray.com/content/mindbox/cloud.mindbox/mobile-sdk/$APP_VERSION/mobile-sdk.aar' 
                    }
                }
            }
        }

    }
    post {
        success {
           slackSend channel: 'jenkins-mindbox', \
           teamDomain: 'umbrellaitcom', \
           color: '#5cb589', \
           message: "MindBox » Android-SDK - ${env.JOB_BASE_NAME} ${env.BUILD_DISPLAY_NAME} Success! (<${env.RUN_DISPLAY_URL}|Open>)", \
           tokenCredentialId: 'umbrella.devops-slack-integration-token'
        }
        failure {
           slackSend channel: 'jenkins-mindbox', \
           teamDomain: 'umbrellaitcom', \
           color: '#951d13', \
           message: "MindBox » Android-SDK - ${env.JOB_BASE_NAME} ${env.BUILD_DISPLAY_NAME} Failed! (<${env.RUN_DISPLAY_URL}|Open>)", \
           tokenCredentialId: 'umbrella.devops-slack-integration-token'
        }
    }
}
