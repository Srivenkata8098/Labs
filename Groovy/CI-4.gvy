pipeline {
    agent { label 'agent-1' }

    environment {
        SONAR_TOKEN = credentials('sonarqube')
    }

    stages {
        stage('checkout code') {
            steps {
                git branch: 'main', url: 'https://github.com/Srivenkata8098/Jenkins_practice.git'
            }
        }

        stage('code compile') {
            steps {
                dir('CI-4') {
                    sh "mvn clean compile"
                }
            }
        }

        stage('code review') {
            steps {
                dir('CI-4') {
                    sh "mvn -P metrics pmd:pmd"
                }
            }
            post {
                success {
                    recordIssues sourceCodeRetention: 'LAST_BUILD', tools: [pmdParser(pattern: '**/pmd.xml')]
                }
            }
        }

        stage('code test') {
            steps {
                dir('CI-4') {
                    sh "mvn test"
                }
            }
            post {
                success {
                    junit 'CI-4/target/surefire-reports/*.xml'
                }
            }
        }

        stage('code coverage') {
            steps {
                dir('CI-4') {
                    sh "mvn verify"
                }
            }
            post {
                success {
                    jacoco changeBuildStatus: true, execPattern: '**/jacoco.exec', runAlways: true, skipCopyOfSrcFiles: true
                }
            }
        }

        stage('static code analysis') {
            steps {
                dir('CI-4') {
                    withSonarQubeEnv('sonarqube') {
                        sh """
                            mvn sonar:sonar \
                              -Dsonar.login=${SONAR_TOKEN} \
                              -Dsonar.projectKey=Srivenkata8098_Jenkins_practice \
                              -Dsonar.organization=srivenkata8098 \
                              -Dsonar.host.url=https://sonarcloud.io
                        """
                    }
                }
            }
        }

        stage('package') {
            steps {
                dir('CI-4') {
                    sh "mvn package"
                }
            }
        }

        stage('security vulnerability scan') {
            steps {
                dir('CI-4') {
                    sh "snyk test"
                }
            }
        }
    }
}

/*apt install -y nodejs npm
npm install -g snyk
snyk auth <your_snyk_token>
snyk test
--plugins--
sonar scanner
snyk 
*/
