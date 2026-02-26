pipeline {
    agent any

    environment {
        JAVA_HOME = "/opt/java/openjdk"
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
    }

    stages {

        stage('Clone') {
            steps {
                echo "Code already checked out by Jenkins SCM"
            }
        }

        stage('Verify Environment') {
            steps {
                sh '''
                echo "Java Version:"
                java -version

                echo "Maven Version:"
                mvn -version
                '''
            }
        }

        stage('Build') {
            steps {
                sh '''
                echo "Building Distributed Whiteboard..."
                mvn clean compile
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                echo "Running Tests..."
                mvn test
                '''
            }
        }

        stage('Package') {
            steps {
                sh '''
                echo "Packaging Application..."
                mvn package
                '''
            }
        }
    }

    post {
        success {
            echo "Pipeline executed successfully!"
        }
        failure {
            echo "Pipeline failed!"
        }
    }
}
