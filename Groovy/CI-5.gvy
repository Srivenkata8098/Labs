pipeline {
    agent { label 'agent-1' }

    environment {
        SONAR_TOKEN = credentials('sonarqube')
        SONAR_SCANNER_HOME = "/opt/sonar-scanner"
    }

    stages {
        stage('Code Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/Srivenkata8098/Jenkins_practice.git'
            }
        }

        stage('Install Dependencies') {
            steps {
                sh '''
                if ! command -v python3 &> /dev/null; then
                    echo "Python3 is not installed. Installing..."
                    sudo apt-get update && sudo apt-get install -y python3
                fi

                if ! command -v pip3 &> /dev/null; then
                    echo "pip3 is not installed. Installing..."
                    sudo apt-get install -y python3-pip
                fi

                if ! dpkg -s python3-venv &> /dev/null; then
                    echo "python3-venv is not installed. Installing..."
                    sudo apt-get install -y python3-venv
                fi

                if ! command -v zip &> /dev/null; then
                    echo "zip is not installed. Installing..."
                    sudo apt-get install -y zip
                fi
                '''
            }
        }

        stage('Install Requirements') {
            steps {
                dir('CI-5') {
                    sh '''
                    # Create virtual environment
                    python3 -m venv venv

                    # Install requirements without activating venv
                    venv/bin/pip install --upgrade pip
                    venv/bin/pip install -r requirements.txt
                    '''
                }
            }
        }

        stage('Code Review') {
            steps {
                dir('CI-5') {
                    sh '''
                    venv/bin/pylint **/*.py > 'CI-5/pylint_report.txt' || true
                    '''
                }
            }
            post {
                always {
                    recordIssues tools: [pyLint(pattern: 'CI-5/pylint_report.txt')]
                }
            }
        }

        stage('Code Test') {
            steps {
                dir('CI-5') {
                    sh 'venv/bin/pytest --junitxml=pytest-report.xml'
                }
            }
            post {
                always {
                    junit 'CI-5/pytest-report.xml'
                }
            }
        }

        stage('Code Coverage') {
            steps {
                dir('CI-5') {
                    sh '''
                    venv/bin/coverage run -m pytest
                    venv/bin/coverage xml
                    '''
                }
            }
            post {
                always {
                    cobertura coberturaReportFile: 'CI-5/coverage.xml'
                }
            }
        }

        stage('Static Code Analysis') {
    steps {
        dir('CI-5') {
            withSonarQubeEnv('sonarqube') {
                sh '''
                    $SONAR_SCANNER_HOME/bin/sonar-scanner \
                    -Dsonar.projectKey=Srivenkata8098_Jenkins_practice \
                    -Dsonar.organization=srivenkata8098 \
                    -Dsonar.sources=. \
                    -Dsonar.exclusions=venv/** \
                    -Dsonar.host.url=https://sonarcloud.io \
                    -Dsonar.login=${SONAR_TOKEN}
                '''
               }
            }
          }
       }


        stage('Package') {
            steps {
                dir('CI-5') {
                    sh '''
                    mkdir -p dist
                    zip -r dist/package.zip . -x "dist/*" "*.git*" "__pycache__/*"
                    '''
                }
            }
        }

        stage('Security Vulnerability Scan') {
            steps {
                dir('CI-5') {
                    sh '''
                    snyk auth || true  # Only works if credentials are already set
                    snyk test || true  # Do not fail build on vulnerabilities for now
                    '''
                }
            }
        }
    }
}

