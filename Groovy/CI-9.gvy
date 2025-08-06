pipeline {
    agent {label 'agent-py' }

    environment {
        SONAR_TOKEN = credentials('sonarqube')
        SNYK_TOKEN = credentials('snyk-token')
    }

    stages{
        stage('Install dependencies') {
            steps{
                sh """
                if ! command -v python3 &> /dev/null; then
                    echo "Python3 is not installed.Installing Python3 ..."
                    sudo apt-get update && sudo apt install python3 -y
                fi

                if ! dpkg -s python3-venv &> /dev/null; then
                    echo "python3-venv is not installed. Installing python3-venv..."
                    sudo apt-get install python3-venv -y
                fi

                if ! command -v pip3 &> /dev/null; then
                    echo "pip3 is not installed. Installing pip3..."
                    sudo apt-get install python3-pip -y
                fi

                sudo apt install -y nodejs npm
                sudo npm install -g snyk
                """

            }
        }
        stage('Code checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/Srivenkata8098/Jenkins_practice.git'
            }
        }
        stage('Install requirements') {
            steps {
                dir('CI-9') {
                    sh """
                    python3 -m venv myenv
                    myenv/bin/pip install -r CI-9/requirements.txt
                    """
                }
                
            }
        }
        stage('code review') {
            steps {
                dir('CI-9') {
                    sh """
                    myenv/bin/pylint . > pylint.txt || true
                    """
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'CI-9/pylint.txt', onlyIfSuccessful: true
                }
                failure {
                    echo "Pylint issues found. Check the pylint.txt file."
                }
            }
        }
        stage('code test') {
            steps {
                dir('CI-9') {
                    sh """
                    myenv/bin/pytest --junitxml=test-reports/results.xml
                    """
                }
            }
            post {
                success {
                    junit 'CI-9/test-reports/results.xml'
                }
            }
        }
        stage('code coverage') {
            steps {
                dir('CI-9') {
                    sh """
                    myenv/bin/coverage run -m pytest
                    myenv/bin/coverage xml -o coverage.xml
                    """
                }
            }
            post {
                success {
                    cobertura coberturaReportFile: 'CI-9/coverage.xml'
                }
            }
        }
        stage('static code analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    dir('CI-9') {
                        sh """
                        sonar-scanner \
                        -Dsonar.projectKey=Srivenkata8098_Jenkins_practice \
                        -Dsonar.organization=srivenkata8098 \
                        -Dsonar.sources=. \
                        -Dsonar.exclusions=myenv/** \
                        -Dsonar.host.url=https://sonarcloud.io \
                        -Dsonar.login=${SONAR_TOKEN}
                        """
                    }
                }
            }
        }
        stage('security scan') {
            steps {
                dir('CI-9') {
                    sh """
                    snyk auth ${SNYK_TOKEN}
                    snyk test 
                    """
                }
            }
        }
        stage('docker build') {
            steps {
                dir('CI-9') {
                    sh """
                    docker build . -t python-app:${BUILD_NUMBER}
                    docker run -d --name python-app -p 5000:5000 python-app:${BUILD_NUMBER}
                    """
                }
            }
        }
    }   
}
