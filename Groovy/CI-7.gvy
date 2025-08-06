pipeline {
    agent {
        docker {
            image 'docker8098/custom-java:v1'
            arg -u 'root'
        }
    }
    environment {
        SONAR_TOKEN = credentials('sonar-token')
        SNYK_TOKEN = credentials('snyk-token')
    }
    stages {
        stage('Code compile') {
            steps {
                sh '''
                cd /java
                mvn clean compile
                '''
            }
        }
        stage('code review') {
            steps {
                sh '''
                cd /java
                mvn -P metrics pmd:pmd 
                '''
            }
            post {
                success {
                    recordIssues sourceCodeRetention: 'LAST_BUILD', tools: [pmdParser(pattern: '**/pmd.xml')]
                }
            }
        }

        stage('Code test') {
            steps {
                sh '''
                cd /java
                mvn test
                '''
            }
            post {
                success {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        stage('code coverage') {
            steps {
                sh '''
                cd /java
                mvn verify  
                '''
            }
            post {
                success {
                    jacoco changeBuildStatus: true, execPattern: '**/jacoco.exec', runAlways: true, skipCopyOfSrcFiles: true
                }
            }
        }
        stage('static code analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh '''
                    cd /java
                    sonar-scanner \
                    -Dsonar.projectKey=Srivenkata8098_Jenkins_practice \
                    -Dsonar.organization=srivenkata8098 \
                    -Dsonar.sources=. \
                    -Dsonar.host.url=https://sonarcloud.io \
                    -Dsonar.login=${SONAR_TOKEN}
                    '''
                }
            }
        }
        stage('code package') {
            steps {
                sh '''
                cd /java
                mvn package
                '''
            }
        }
        stage('snyk security scan') {
            steps {
                sh '''
                cd /java
                snyk auth ${SNYK_TOKEN}
                snyk test --file=pom.xml || true
                '''
            }
        }
    }    
}



#####################CI-7- need some changeBuildStatus



pipeline {
    agent {
        docker {
            image 'docker8098/custom-java:v1'
            args "-u root -v ${env.WORKSPACE}:/java"
        }
    }

    stages {
        stage('Code compile') {
            steps {
                sh '''
                cd /java
                mvn clean compile -Dmaven.repo.local=/java/.m2
                ls -R target/
                cp -r target/classes /java/ || echo "No classes found"
                '''
            }
        }

        stage('Code review') {
            steps {
                sh '''
                cd /java
                mvn -P metrics pmd:pmd -Dmaven.repo.local=/java/.m2
                cp target/pmd.xml /java/
                '''
            }
            post {
                success {
                    recordIssues sourceCodeRetention: 'LAST_BUILD', tools: [pmdParser(pattern: '**/pmd.xml')]
                }
            }
        }

        stage('Code test') {
            steps {
                sh '''
                cd /java
                mvn test -Dmaven.repo.local=/java/.m2
                cp -r target/surefire-reports /java/
                '''
            }
            post {
                success {
                    junit 'surefire-reports/*.xml'
                }
            }
        }

        stage('Code coverage') {
            steps {
                sh '''
                cd /java
                mvn verify -Dmaven.repo.local=/java/.m2
                cp target/jacoco.exec /java/
                '''
            }
            post {
                success {
                    jacoco changeBuildStatus: true, execPattern: '**/jacoco.exec', runAlways: true, skipCopyOfSrcFiles: true
                }
            }
        }

        stage('Static code analysis') {
            steps {
                withCredentials([string(credentialsId: 'sonarqube', variable: 'SONAR_TOKEN')]) {
                    withSonarQubeEnv('sonarqube') {
                        sh '''
                        cd /java
                        sonar-scanner \
                          -Dsonar.projectKey=Srivenkata8098_Jenkins_practice \
                          -Dsonar.organization=srivenkata8098 \
                          -Dsonar.sources=. \
                          -Dsonar.java.binaries=target/classes \
                          -Dsonar.exclusions=**/*.js,**/*.ts \
                          -Dsonar.host.url=https://sonarcloud.io \
                          -Dsonar.login=$SONAR_TOKEN
                        '''
                    }
                }
            }
        }

        stage('Code package') {
            steps {
                sh '''
                cd /java
                mvn package -Dmaven.repo.local=/java/.m2
                '''
            }
        }

        stage('Snyk security scan') {
            steps {
                withCredentials([string(credentialsId: 'snyk-token', variable: 'SNYK_TOKEN')]) {
                    sh '''
                    cd /java
                    snyk auth $SNYK_TOKEN
                    snyk test --file=pom.xml || true
                    '''
                }
            }
        }
    }
}

