pipeline {
    agent {
        docker {
            image 'docker8098/cicd_java:v2'
            args '-u root -v /var/run/docker.sock:/var/run/docker.sock'
        }
    }

    environment {
        HOME = '/root'
        SONAR_TOKEN = credentials('sonarqube')
        SNYK_TOKEN = credentials('snyk-token')
        DOCKERHUB_TOKEN = credentials('dockerhub-cred')

        IMAGE_NAME = 'docker8098/cicd-1-java'
        IMAGE_TAG = "v${BUILD_NUMBER}"
    }

    stages {
        stage('code checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/Srivenkata8098/Jenkins_practice.git'
            }
        }
        stage('code compile') {
            steps {
                dir('CICD') {
                    sh """
                    mvn clean compile 
                    """
                }
            }
        }
        stage('code review') {
            steps {
                dir('CICD') {
                    sh """
                    mvn -P metrics pmd:pmd -Dmaven.repo.local=$WORKSPACE
                    """
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
                dir('CICD') {
                    sh """
                    mvn test -Dmaven.repo.local=$WORKSPACE
                    """
                }
            }
            post {
                success {
                    junit 'CICD/target/surefire-reports/*.xml'
                }
            }
        }
        stage('code coverage') {
            steps {
                dir('CICD') {
                    sh """
                    mvn verify -Dmaven.repo.local=$WORKSPACE
                    """
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
                dir('CICD') {
                    withSonarQubeEnv('sonarqube'){
                        sh """
                        mvn sonar:sonar \
                        -Dsonar.projectKey=Srivenkata8098_Jenkins_practice \
                        -Dsonar.organization=srivenkata8098 \
                        -Dsonar.sources=src/main/java \
                        -Dsonar.tests=src/test/java \
                        -Dsonar.exclusions=**/*.js,**/*.ts \
                        -Dsonar.host.url=https://sonarcloud.io \
                        -Dsonar.login=${SONAR_TOKEN}
                        """
                    }
                }
            }
        }
        stage('code package') {
            steps {
                dir('CICD') {
                    sh """
                    mvn clean package --no-transfer-progress -Dmaven.repo.local=$WORKSPACE
                    """
                }
            }
        }
        stage('security scan') {
            steps {
                dir('CICD') {
                    sh """
                    snyk auth ${SNYK_TOKEN}
                    snyk test || true
                    """
                }
            }
        }
        stage('docker build') {
            steps {
                dir('CICD') {
                    sh """
                    cp target/demo-1.0.0.war .
                    docker rm -f cicd || true
                    docker build . -t ${IMAGE_NAME}:${IMAGE_TAG}
                    docker run -d --name cicd -p 8080:8080 ${IMAGE_NAME}:${IMAGE_TAG}
                    """
                }
            }
        }
        stage('docker push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-cred', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                sh """
                echo "${DOCKER_PASSWORD}" | docker login -u "${DOCKER_USERNAME}" --password-stdin
                docker push ${IMAGE_NAME}:${IMAGE_TAG}
                """
                }
            }
        }
        stage('updating manifests') {
            steps {
                sh "sed -i 's|newTag: .*|newTag: ${IMAGE_TAG} |g' CICD/kustomize/base/kustomization.yml"
                sh "sed -i 's|newTag: .*|newTag: ${IMAGE_TAG} |g' CICD/kustomize/overlay/dev/kustomization.yml"
                sh "sed -i 's|newTag: .*|newTag: ${IMAGE_TAG} |g' CICD/kustomize/overlay/prod/kustomization.yml"

                sh """
                echo "=== base/kustomization.yml ==="
                cat CICD/kustomize/base/kustomization.yml
    
                echo "=== overlay/dev/kustomization.yml ==="
                cat CICD/kustomize/overlay/dev/kustomization.yml
        
                echo "=== overlay/prod/kustomization.yml ==="
                cat CICD/kustomize/overlay/prod/kustomization.yml
                """
            }
        }
        stage('Git commit and push') {
            steps {
                sshagent(['jenkins-sshkey']) {
                    sh """
                    
                    mkdir -p ~/.ssh
                    
                    ssh-keyscan github.com >> ~/.ssh/known_hosts
                    
                    git clone git@github.com:Srivenkata8098/Jenkins_practice.git repo
                    cd repo
                    
                    git config user.email "narayana8098@gmail.com"
                    git config user.name "Srivenkata8098"
                    git checkout -b update-manifests_${BUILD_NUMBER}
                    
                    cp -r ../CICD/kustomize/base/* CICD/kustomize/base/
                    cp -r ../CICD/kustomize/overlay/dev/* CICD/kustomize/overlay/dev/
                    cp -r ../CICD/kustomize/overlay/prod/* CICD/kustomize/overlay/prod/


                    git add CICD/kustomize/base/kustomization.yml
                    git add CICD/kustomize/overlay/dev/kustomization.yml
                    git add CICD/kustomize/overlay/prod/kustomization.yml


                    if git diff --cached --quiet; then
                        echo "No changes to commit"
                    else
                        git commit -m "Update manifests with new image tag ${IMAGE_TAG}"
                        git push -u origin update-manifests_${BUILD_NUMBER}
                    fi
                    """
                }
            }    
        }
    }
}
