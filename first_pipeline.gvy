pipeline {
    agent any

    stages {
        stage('Compile') {
            steps {
                echo 'Hii! I am your first step in the pipeline.'
                git 'https://github.com/Srivenkata8098/samplejavaapp.git'
                sh '/opt/maven/bin/mvn compile'   
            }
        }
        stage('code-review') {
            steps {
                echo 'Hii! I am your first step in the pipeline.'
                sh '/opt/maven/bin/mvn -P metrics pmd:pmd'
            }
            post {
                success {
                  recordIssues sourceCodeRetention: 'LAST_BUILD', tools: [pmdParser(pattern: '**/pmd.xml')]
                }
            }
        }
        stage('code-test') {
            steps {
                echo 'Hii! I am your first step in the pipeline.'
                sh '/opt/maven/bin/mvn test'
            }
            post {
                success {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }    
        stage('code-coverage') {
            steps {
                echo 'Hii! I am your first step in the pipeline.'
                sh '''/opt/maven/bin/mvn verify'''
            }
            post {
                success {
                    jacoco changeBuildStatus: true, runAlways: true, skipCopyOfSrcFiles: true
                }   
            }
        }    
        stage('code-package') {
            steps {
                echo 'Hii! I am your first step in the pipeline.'
                sh '''/opt/maven/bin/mvn package'''
            }
        }
    }
}