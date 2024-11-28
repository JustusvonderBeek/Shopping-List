pipeline {
    agent any

    environment {
        APP_BUILD_FILE = 'app/build/outputs/apk/release/app-release-unsigned.apk'
        APP_CERTIFICATE_PATH = 'app/src/main/res/raw/shoppinglist.crt'
    }
    stages {
        stage('Build') {
            steps {
                echo 'Building..'
                withCredentials([file(credentialsId: '1b0afdc2-8003-4798-908e-094593079784', variable: 'certificate')]) {
                    sh 'cp $certificate "${WORKSPACE}/${APP_CERTIFICATE_PATH}"'
                }
                sh './gradlew assembleRelease'
//                 echo 'Signing the release...'
//                 withCredentials([usernamePassword(credentialsId: 'cd37cd00-a4d7-4499-a943-7344f7d1ab85', passwordVariable: 'STORE_PASSWORD', usernameVariable: 'storePassword'), usernamePassword(credentialsId: 'cd37cd00-a4d7-4499-a943-7344f7d1ab85', passwordVariable: 'TEST123', usernameVariable: 'KEY_PASSWORD')]) {
//                     sh './gradlew bundleRelease'
//                 }
            }
        }
        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }
        stage('Prepare Deploy') {
            steps {
                echo 'Checking if files for deployment are available...'
                script {
                    def releaseAppArtifactExists = fileExists("${WORKSPACE}/${APP_BUILD_FILE}")
                    if (!releaseAppArtifactExists) {
                        echo 'Build release app not found! Aborting pipeline...'
                        exit 1
                    }
                }
                echo 'Build release app found'
            }
        }
        stage ('Cleanup') {
            steps {
                sh 'rm "${WORKSPACE}/${APP_CERTIFICATE_PATH}"'
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully'
            echo 'Publishing to Google Play Console...'
            androidApkUpload googleCredentialsId: 'dca2671a-6038-4915-a00c-b01a29e11c74', releaseName: 'Version {versionName}', rolloutPercentage: '0', trackName: 'internal'
        }
        failure {
            echo 'Pipeline failed!'
            script {
                def certificateLocation = "${WORKSPACE}/${APP_CERTIFICATE_PATH}"
                if (fileExists(certificateLocation)) {
                    sh 'rm certificateLocation'
                }
            }
        }
    }
}