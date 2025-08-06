pipeline {
    agent any

    stages {
        stage('clean the workspace') {
            steps {
                cleanWs()
            }
        }
        stage('source code') {
            steps {
                git branch : 'main', url: 'https://github.com/Srivenkata8098/Jenkins_practice.git'
            }
        }
        stage('compile'){
            steps {
                dir('CI-1'){
                    sh 'mvn compile'
                }
                
            }
        }
        stage('test') {
            steps {
                dir('CI-1'){
                    sh 'mvn test'
                }
            }
        }
        stage('package') {
            steps {
                dir('CI-1'){
                    sh 'mvn package'
                }
            }
        }
        stage('artifacts') {
            steps {
                archiveArtifacts artifacts: 'CI-1//target/*.jar', followSymlinks: false
            }
        }
    }
}

