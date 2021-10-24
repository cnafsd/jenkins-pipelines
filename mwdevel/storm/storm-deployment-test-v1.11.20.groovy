#!/usr/bin/env groovy

@Library('sd')_
def kubeLabel = getKubeLabel()
def runner_job

pipeline {
  agent {
    kubernetes {
      label "${kubeLabel}"
      cloud 'Kube mwdevel'
      defaultContainer 'runner'
      inheritFrom 'ci-template'
    }
  }

  options {
    buildDiscarder(logRotator(numToKeepStr: '5'))
  }

  triggers {
    cron('@daily')
  }

  stages {
    stage("test-pkg-storm-v1.11.20") {
      steps {
        script {
          catchError{
            runner_job = build job: "storm-deployment-tests/master", 
              parameters: [
                string(name: 'STORM_TARGET_RELEASE', value: "stable"),
                string(name: 'VOMS_TARGET_RELEASE', value: "stable"),
                string(name: 'PKG_STORM_BRANCH', value: "v1.11.20"),
                string(name: 'PKG_VOMS_BRANCH', value: "none"),
                string(name: 'PUPPET_MODULE_BRANCH', value: "v4"),
              ], 
              wait: true, 
              propagate: false
          }
          step ([$class: 'CopyArtifact',
            projectName: "storm-deployment-tests/master",
            selector: [$class: 'SpecificBuildSelector', buildNumber: "${runner_job.number}"]
          ])

          archiveArtifacts 'all-in-one/centos7/output/**'
          step([$class: 'RobotPublisher',
            disableArchiveOutput: false,
            logFileName: 'log.html',
            otherFiles: '*.png',
            outputFileName: 'output.xml',
            outputPath: "all-in-one/centos7/output/reports",
            passThreshold: 100,
            reportFileName: 'report.html',
            unstableThreshold: 90])
        }
      }
    }
  }
}
