#!/usr/bin/env groovy

pipeline {
  agent { label 'docker' }

  options {
    timeout(time: 3, unit: 'HOURS')
    buildDiscarder(logRotator(numToKeepStr: '5'))
  }

  triggers {
    cron('@daily')
  }

  parameters {
    string(name: 'TESTSUITE_BRANCH', defaultValue: 'nightly', description: 'Which branch of storm-testsuite_runner' )
    string(name: 'TESTSUITE_EXCLUDE', defaultValue: "to-be-fixedORcdmi", description: '')
    string(name: 'TESTSUITE_SUITE', defaultValue: "tests", description: '')
  }

  environment {
    JOB_NAME = 'storm-testsuite_runner'
  }

  stages {
    stage('run-testsuite') {
      steps {
        script {
          catchError{
            runner_job = build job: "${env.JOB_NAME}/${params.TESTSUITE_BRANCH}", propagate: false, parameters: [
              string(name: 'STORM_BACKEND_HOSTNAME', value: "omii005-vm03.cnaf.infn.it"),
              string(name: 'STORM_FRONTEND_HOSTNAME', value: "omii005-vm03.cnaf.infn.it"),
              string(name: 'STORM_WEBDAV_HOSTNAME', value: "omii005-vm03.cnaf.infn.it"),
              string(name: 'STORM_GRIDFTP_HOSTNAME', value: "omii005-vm03.cnaf.infn.it"),
              string(name: 'CDMI_ENDPOINT', value: "omii003-vm01.cnaf.infn.it:8888"),
              string(name: 'TESTSUITE_EXCLUDE', value: "${params.TESTSUITE_EXCLUDE}"),
              string(name: 'TESTSUITE_SUITE', value: "${params.TESTSUITE_SUITE}"),
              string(name: 'STORM_STORAGE_ROOT_DIR', value: "/storage"),
            ]
          }
        
          step ([$class: 'CopyArtifact',
            projectName: "${env.JOB_NAME}/${params.TESTSUITE_BRANCH}",
            selector: [$class: 'SpecificBuildSelector', buildNumber: "${runner_job.number}"]
          ])

          archiveArtifacts '**'
          step([$class: 'RobotPublisher',
            disableArchiveOutput: false,
            logFileName: 'log.html',
            otherFiles: '*.png',
            outputFileName: 'output.xml',
            outputPath: "runner/reports-jenkins-storm-testsuite_runner-nightly-${runner_job.number}/reports",
            passThreshold: 100,
            reportFileName: 'report.html',
            unstableThreshold: 90])
        }
      }
    }
  }
}
