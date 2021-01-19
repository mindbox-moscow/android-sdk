pipeline {
  environment {
     APP_NAME = 'mindbox-android-sdk'
  }
   agent none
   stages {
        stage('Preflight check'){
            agent {node {label 'hetzner-agent-1'}}
            when { anyOf { branch 'develop'; branch 'release'; branch 'pipeline'} }
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

       stage('MindBox Android SDK tests'){
           when { anyOf { branch 'develop'; branch 'release'; branch 'pipeline'} }
           parallel {
                stage ('Gradle Lint') {
                    when { anyOf { branch 'develop'; branch 'release'; branch 'pipeline'} }
                    agent {node {label 'hetzner-agent-1' }}
                    steps {
                        sh label: 'Running lint check', script: './gradlew check'
                    }
                }
                stage ('Unit Tests') {
                    when { anyOf { branch 'develop'; branch 'release'; branch 'pipeline'} }
                    agent {node {label 'hetzner-agent-1' }}
                    steps {
                        sh label: 'Running Unit test', script: './gradlew test'
                    }
                }
           }
        }

        stage('Build MindBox SDK'){
            agent {node {label 'hetzner-agent-1' }}
            when { anyOf { branch 'develop'; branch 'pipeline'} }
            steps {
                script {
                    sshagent (credentials: ['umbrella-roman-parfinenko']) {
                        sh label: 'Project cleanup', script:  './gradlew clean'
                        sh label: 'Project build', script:  './gradlew build'
                        sh label: 'Build a debug APK', script:  './gradlew assembleDebug'
                        sh label: 'Assembles Test applications', script:  './gradlew assembleAndroidTest'
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
