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
                withCredentials([file(credentialsId: 'shopping-list-certificate', variable: 'certificate')]) {
                    sh 'cp $certificate "${WORKSPACE}/${APP_CERTIFICATE_PATH}"'
                }
                echo 'Bundling APK into AAB for uploading...'
                withCredentials([usernamePassword(credentialsId: 'shopping-list-app-keystore-password', passwordVariable: 'keystorePassword'),
                usernamePassword(credentialsId: 'shopping-list-app-signing-key', passwordVariable: 'signingKeyAlias'),
                usernamePassword(credentialsId: 'shopping-list-app-signing-key-password', passwordVariable: 'signingKeyPassword')]) {
                    sh 'echo "storeFile=${APP_KEYSTORE_FILE}" > ${WORKSPACE}/keystore.properties'
                    sh 'echo "storePassword=${keystorePassword}" >> ${WORKSPACE}/keystore.properties'
                    sh 'echo "keyAlias=${signingKeyAlias}" >> ${WORKSPACE}/keystore.properties'
                    sh 'echo "keyPassword=${signingKeyPassword}" >> ${WORKSPACE}/keystore.properties'
                    sh './gradlew bundleRelease'
                }
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