pipeline {
    agent any

    environment {
        ANDROID_HOME = "${env.JENKINS_HOME}/tools/android-sdk"
    }
    stages {
        stage('Build') {
            steps {
                echo 'Building..'
                sh './gradlew assembleRelease'
            }
        }
        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }
        stage('Publish') {
            steps {
                echo 'Publishing....'
            }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying....'
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}