pipeline {
    agent { label 'agent'}

    environment {
        SONAR_TOKEN = credentials('sonarqube')
        SNYK_TOKEN = credentials('snyk-token')
    }

    stages{
        stage('code checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/Srivenkata8098/Jenkins_practice.git'
            }
        }
        stage('code compile') {
            steps {
                dir('CI-8') {
                    sh '''
                    mvn clean compile
                    '''
                }
            }
        }
        stage('code review') {
            steps {
                dir('CI-8') {
                    sh '''
                    mvn -P metrics pmd:pmd
                    '''
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
                dir('CI-8') {
                    sh '''
                    mvn test
                    '''           
                }
            }
            post {
                success {
                    junit 'CI-8/target/surefire-reports/*.xml'
                }
            }
        }
        stage('code coverage') {
            steps {
                dir('CI-8') {
                    sh '''
                    mvn verify
                    ''' 
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
                dir('CI-8') {
                    withSonarQubeEnv('sonarqube') {
                        sh """
                            mvn sonar:sonar \
                              -Dsonar.login=${SONAR_TOKEN} \
                              -Dsonar.projectKey=Srivenkata8098_Jenkins_practice \
                              -Dsonar.organization=srivenkata8098 \
                              -Dsonar.exclusions=**/*.js,**/*.ts \
                              -Dsonar.host.url=https://sonarcloud.io
                        """
                    }
                }
            }
        }
        stage('code package') {
            steps {
                dir('CI-8') {
                    sh '''
                    mvn package
                    '''
                }
            }
        }
        stage('code scan') {
            steps {
                dir('CI-8') {
                    sh '''
                    snyk auth $SNYK_TOKEN
                    snyk test --file=pom.xml || true
                    '''
                }
            }
        }
        stage('docker build') {
            steps {
                dir('CI-8') {
                    sh '''
                    cp target/demo-1.0.0.war .
                    docker build . -t java-app:${BUILD_NUMBER}
                    docker run -d --name java-app -p 8080:8080 java-app:${BUILD_NUMBER}
                    '''
                }
            }
        }
    }
}

// http://13.222.168.67:8080/demo-1.0.0/hello
