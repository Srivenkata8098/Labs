pipeline {
    agent {
        docker {
            image 'docker8098/custom-docker-image:v1'
             args '-u root'
        }
    }
    environment {
        SONAR_TOKEN = credentials('sonarqube')
        SNYK_AUTH_TOKEN = credentials('snyk-token')
    }
    stages {
        stage('Code Review') {
            steps {
                sh 'cd /app'
                sh 'pylint --output-format=parseable **/*.py > pylint_report.txt || true'
            }
            post {
                success {
                    recordIssues tools: [pyLint(pattern: 'pylint_report.txt')]
                }
            }
        }
        stage('Code Test') {
            steps {
                sh '''
                 cd /app
                 pytest --junitxml=/home/devops/jenkins/workspace/CI-6/pytest-report.xml
                 '''
            }
            post {
                success {
                    junit 'pytest-report.xml'
                }
            }
        }
        stage('Code Coverage') {
            steps {
                sh '''
                    cd /app
                    coverage run -m pytest
                    coverage xml -o $WORKSPACE/coverage.xml
                '''
            }
            post {
                success {
                    cobertura coberturaReportFile: 'coverage.xml'
                }
            }
        }
        stage('Static Code Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh '''
                    cd /app
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
        stage('Snyk Security Scan') {
            steps {
                sh '''
            echo "Listing files in /app:"
            ls -l /app

            cd /app
            snyk auth ${SNYK_AUTH_TOKEN}
            snyk test --file=requirements.txt || true
            '''
            }
        }
    }
}

