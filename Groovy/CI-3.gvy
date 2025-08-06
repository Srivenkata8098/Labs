pipeline {
    agent any

    stages {
        stage('checkout code') {
            steps {
                git branch: 'main', url: 'https://github.com/Srivenkata8098/Jenkins_practice.git'
            }
        }
        stage('compile') {
            steps {
                dir('CI-3') {
                    sh '${MAVEN_HOME}/bin/mvn clean compile'
                }               
            }
        }
        stage('code review') {
            steps {
                dir('CI-3') {
                    sh '${MAVEN_HOME}/bin/mvn -P metrics pmd:pmd'
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
                dir('CI-3') {
                    echo 'Running tests...'
                    sh '${MAVEN_HOME}/bin/mvn test'
                }
            }
            post {
                success {
                    junit 'CI-3/target/surefire-reports/*.xml'
                }
            }
        }
        stage('code coverage') {
            steps {
                dir('CI-3') {
                    echo 'Calculating code coverage...'
                    sh '${MAVEN_HOME}/bin/mvn verify'
                }
            }
            post {
                success {
                    jacoco changeBuildStatus: true, execPattern: '**target/jacoco.exec', runAlways: true, skipCopyOfSrcFiles: true
                }
            }
        }
        stage('static code analysis') {
            steps {
                dir('CI-3') {
                    echo 'Performing static code analysis...'
                    sh '${MAVEN_HOME}/bin/mvn checkstyle:checkstyle'
                }
            }
            post {
                success {
                    recordIssues tools: [
                        checkStyle(pattern: 'target/checkstyle-result.xml')
                    ]
                }
            }
        }
        stage('Package application') {
            steps {
                dir('CI-3') {
                    echo 'Packaging application...'
                    sh '${MAVEN_HOME}/bin/mvn package'
                }
            }
        }
        stage('security vulnerability scan') {
            steps {
                dir('CI-3') {
                    echo 'Scanning for security vulnerabilities...'
                    sh '${MAVEN_HOME}/bin/mvn org.owasp:dependency-check-maven:check'
                }
            }
            post {
                success {
                    dependencyCheckPublisher pattern: '**/dependency-check-report.html'
                }
            }
        }
    }
}



/*owasp, Warnings, jacoco,*/ 
