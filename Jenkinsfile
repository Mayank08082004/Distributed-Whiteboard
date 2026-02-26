node {
    try {

        stage('Clone') {
            echo "Code already checked out by Jenkins SCM"
        }

        stage('Verify Environment') {
            sh '''
            echo "Java Version:"
            java -version

            echo "Maven Version:"
            mvn -version
            '''
        }

        stage('Build') {
            sh '''
            echo "Building Distributed Whiteboard..."
            mvn clean compile
            '''
        }

        stage('Test') {
            sh '''
            echo "Running Tests..."
            mvn test
            '''
        }

        stage('Package') {
            sh '''
            echo "Packaging Application..."
            mvn package
            '''
        }

        echo "Pipeline executed successfully!"

    } catch (Exception e) {
        echo "Pipeline failed!"
        throw e
    }
}
