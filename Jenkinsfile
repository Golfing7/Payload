pipeline {
  agent any

  tools {
    jdk 'OpenJDK'
    maven 'MQMaven'
  }

  stages {
    stage('Build') {
      steps {
        git(url: 'https://github.com/Mine-Quest/Payload.git', branch: 'main', credentialsId: 'Iv1.88498de6720734a8')
        sh 'mvn clean install'
      }
    }

    stage('Archive') {
      steps {
        archiveArtifacts(artifacts: 'target/Payload.jar', onlyIfSuccessful: true)
      }
    }

    stage('Nexus Publish') {
      steps {
       nexusPublisher nexusInstanceId: '1', nexusRepositoryId: 'maven-releases', packages: [[$class: 'MavenPackage', mavenAssetList: [[classifier: '', extension: '', filePath: '/var/lib/jenkins/workspace/MQ_Payload_main/target/Payload.jar']], mavenCoordinate: [artifactId: 'Payload', groupId: 'com.jonahseguin', packaging: 'jar', version: '3.1.0']]]
      }
    }
  }
}