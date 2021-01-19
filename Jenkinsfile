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
                        sh label: 'Running lint check', script: './gredlew check'
                    }
                }
                stage ('Unit Tests') {
                    when { anyOf { branch 'develop'; branch 'release'; branch 'pipeline'} }
                    agent {node {label 'hetzner-agent-1' }}
                    steps {
                        sh label: 'Running Unit test', script: './gredlew test'
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
                        sh label: 'Project cleanup', script:  './gredlew clean'
                        sh label: 'Project build', script:  './gredlew build'
                        sh label: 'Build a debug APK', script:  './gredlew assembleDebug'
                        sh label: 'Assembles Test applications', script:  './gredlew assembleAndroidTest'
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
