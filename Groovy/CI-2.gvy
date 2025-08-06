pipeline {
    agent any

    environment {
        DOCKERHUB_CREDENTIALS = 'docker_hub_credentials'
        DOCKER_IMAGE_NAME = 'docker8098/apache_custom_1'
        TAG = "v${BUILD_NUMBER}"
    }

    stages {
        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Checkout Git Repo') {
            steps {
                git branch: 'main', url: 'https://github.com/Srivenkata8098/Jenkins_practice.git'
            }
        }

        stage('Build and Push Docker Image') {
            steps {
                script {
                    // Navigate into the CI-2 directory
                    dir('CI-2') {
                        // Build the image
                        def custom_image = docker.build("${DOCKER_IMAGE_NAME}:${TAG}")

                        // Login and Push inside the same script block
                        withDockerRegistry(credentialsId: "${DOCKERHUB_CREDENTIALS}", url: 'https://index.docker.io/v1/') {
                            custom_image.push()
                        }
                    }
                }
            }
        }
        stage('Running the container') {
            steps {
                sh "docker run -d -p 89:80 --name apache${BUILD_NUMBER} ${DOCKER_IMAGE_NAME}:${TAG}"
               }
            }
        }
    }



