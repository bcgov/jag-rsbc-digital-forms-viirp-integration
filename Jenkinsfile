pipeline {
    agent none
    options {
        disableResume()
        timeout(time: 24, unit: 'HOURS')
    }

    environment {
        API_RELEASE_VERSION = '1'
    }
    stages {

        stage('Build and Deploy Inputs') {
           agent none
           steps {
               confirm_build()
           }
        }

        // stage('Run Unit Tests'){
        //     agent { label 'master' }
        //     steps{
        //         script{
        //             sh """
        //                 cd digitalforms-api
        //                 mvn --batch-mode test
        //             """
        //         }
        //     }
        // }


        stage('Build') {
            agent { label 'master' }
            options { skipDefaultCheckout(false) }
            when {
                expression { return env.SKIP_BUILD == "false";}
                beforeInput true
            }
            // oc start-build vips-api-build --follow --wait
            steps {
                script {
                    sh """                    
                    cd openshift
                    oc process -f api-build.yml --param-file build-params.yml --param VERSION=build-${env.CHANGE_ID} --param SUFFIX=-build-${env.CHANGE_ID} --param SOURCE_REPOSITORY_REF=${env.CHANGE_BRANCH} | oc apply -f -
                    
                    oc start-build api-test-buildconfig --follow --wait
                    """
                }

            }
        }

        stage('Scale Down PR') {
            agent { label 'master' }
            when {
                allOf {
                     expression { env.SKIP_PR != "true" }
                 }
                beforeInput true
            }
            steps {
                echo "Checking existing PR.."
                script{
                    def PR_Deploy_STATUS = sh ( script: "cd openshift && oc get deploy -n c220ad-dev vips-api-deployment-pr-${env.CHANGE_ID}-${env.API_RELEASE_VERSION}  -o jsonpath='{.metadata.name}'", returnStatus: true )
                    if(PR_Deploy_STATUS==1){
                        echo "No existing PR environments to scale down!!"
                    }else{
                        sh """
                            oc scale -n c220ad-dev deploy/vips-api-deployment-pr-${env.CHANGE_ID}-${env.API_RELEASE_VERSION}  --replicas=0

                        """
                    }
                }
            }
        }

        
        stage('Deploy (PR)') {
            agent { label 'master' }
            when {
                allOf {
                     expression { env.SKIP_PR != "true" }
                 }
                beforeInput true
            }
            input {
                message "Should we continue with deployment to PR?"
                ok "Yes!"
            }
            steps {
                echo "Deploying PR ..."
                script {
                    sh """                    
                    cd openshift
                    oc process -f api-deploy.yml --param-file pr-deploy-params.yml --param SUFFIX=-pr-${env.CHANGE_ID} --param BUILD_VERSION=${env.CHANGE_ID} --param API_VERSION=${env.API_RELEASE_VERSION} | oc apply -f -
                    oc rollout status -n c220ad-dev deploy/vips-api-deployment-pr-${env.CHANGE_ID}-${env.API_RELEASE_VERSION} 
                    """
                }
            }
        }

        
        stage('Deploy (DEV)') {
            agent { label 'master' }
            when {
                allOf {
                     expression { env.SKIP_DEV != "true" }
                     expression { env.CHANGE_TARGET == 'master' }
                 }
                beforeInput true
            }
            input {
                message "Should we continue with deployment to DEV?"
                ok "Yes!"
            }
            
            stages{
                    stage('Scale down Dev'){
                    steps{
                        echo "Checking existing Dev.."
                        script{
                            def DEV_Deploy_STATUS = sh ( script: "cd openshift && oc get deploy -n c220ad-dev vips-api-deployment-dev-${env.API_RELEASE_VERSION}  -o jsonpath='{.metadata.name}'", returnStatus: true )
                            if(DEV_Deploy_STATUS==1){
                                echo "No existing Dev deployments to scale down!!"
                            }else{
                                sh """
                                oc scale -n c220ad-dev deploy/vips-api-deployment-dev-${env.API_RELEASE_VERSION} --replicas=0
                                """
                            }
                        }
                    }
                    }

                    stage('Deploy (Dev)'){
                        steps{
                            echo "Deploying Dev ..."
                            script {
                                sh """                    
                                cd openshift
                                oc process -f api-deploy.yml --param-file dev-deploy-params.yml --param SUFFIX=-dev --param BUILD_VERSION=${env.CHANGE_ID} --param API_VERSION=${env.API_RELEASE_VERSION}  | oc apply -f -
                                oc rollout status -n c220ad-dev deploy/vips-api-deployment-dev-${env.API_RELEASE_VERSION} 
                                """
                            }
                        }
                    }            
            }
        }




        stage('Deploy (TEST)') {
            agent { label 'master' }
            when {
                allOf {
                     expression { env.SKIP_TEST != "true" }
                     expression { env.CHANGE_TARGET == 'master' }
                 }
                beforeInput true
            }
            input {
                message "Should we continue with deployment to TEST?"
                ok "Yes!"
            }
            stages{
                    stage('Scale down Test'){
                    steps{
                        echo "Checking existing Test.."
                        script{
                            def DEV_Deploy_STATUS = sh ( script: "cd openshift && oc get deploy -n c220ad-test vips-api-deployment-test-${env.API_RELEASE_VERSION} -o jsonpath='{.metadata.name}'", returnStatus: true )
                            if(DEV_Deploy_STATUS==1){
                                echo "No existing Test deployments to scale down!!"
                            }else{
                                sh """
                                oc scale -n c220ad-test deploy/vips-api-deployment-test-${env.API_RELEASE_VERSION} --replicas=0
                                """
                            }
                        }
                    }
                    }

                    stage('Deploy (Test)'){
                        steps{
                            echo "Deploying Test ..."
                            script {
                                sh """                    
                                cd openshift
                                oc process -f api-deploy.yml --param-file test-deploy-params.yml --param SUFFIX=-test --param BUILD_VERSION=${env.CHANGE_ID} --param API_VERSION=${env.API_RELEASE_VERSION} | oc apply -f -
                                oc rollout status -n c220ad-test deploy/vips-api-deployment-test-${env.API_RELEASE_VERSION}
                                """
                            }
                        }
                    }            
            }
        }



        stage('Deploy (PROD)') {
            agent { label 'master' }
            when {
                allOf {
                     expression { env.SKIP_PROD != "true" }
                     expression { env.CHANGE_TARGET == 'master' }
                 }
                beforeInput true
            }
            input {
                message "Should we continue with deployment to PROD?"
                ok "Yes!"
            }
            stages{
                    stage('Scale down Prod'){
                    steps{
                        echo "Checking existing Prod.."
                        script{
                            def DEV_Deploy_STATUS = sh ( script: "cd openshift && oc get deploy -n c220ad-prod vips-api-deployment-prod-${env.API_RELEASE_VERSION} -o jsonpath='{.metadata.name}'", returnStatus: true )
                            if(DEV_Deploy_STATUS==1){
                                echo "No existing Prod deployments to scale down!!"
                            }else{
                                sh """
                                oc scale -n c220ad-prod deploy/vips-api-deployment-prod-${env.API_RELEASE_VERSION} --replicas=0
                                """
                            }
                        }
                    }
                    }

                    stage('Deploy (Prod)'){
                        steps{
                            echo "Deploying Prod ..."
                            script {
                                sh """                    
                                cd openshift
                                oc process -f api-deploy.yml --param-file prod-deploy-params.yml --param SUFFIX=-prod --param BUILD_VERSION=${env.CHANGE_ID} --param API_VERSION=${env.API_RELEASE_VERSION} | oc apply -f -
                                oc rollout status -n c220ad-prod deploy/vips-api-deployment-prod-${env.API_RELEASE_VERSION}
                                """
                            }
                        }
                    }            
            }
        }
    }
}






// ###################################pipeline scripts#########################

void confirm_build(){
  script {
      def INPUT_PARAMS
      if (env.CHANGE_TARGET == null || env.CHANGE_BRANCH == null){
        currentBuild.result = 'ABORTED'
        echo('Skipping non PR change')
        env.SKIP_PR = "true";
        env.SKIP_DEV = "true";
        env.SKIP_PROD = "true";
        env.SKIP_TEST = "true";
        return
      }
      if (env.CHANGE_TARGET == 'master' && env.CHANGE_BRANCH.indexOf('patch/') != 0) {
        INPUT_PARAMS = input message: 'Build and deploy the latest changes?', ok: 'Yes',
            parameters: [
                booleanParam(defaultValue: false, name: 'SKIP_PR', description: 'Skip PR Deployment'),
                booleanParam(defaultValue: false, name: 'SKIP_DEV', description: 'Skip Dev Deployment'),
                booleanParam(defaultValue: false, name: 'SKIP_TEST', description: 'Skip Test Deployment'),
                booleanParam(defaultValue: false, name: 'SKIP_PROD', description: 'Skip Prod Deployment'),
                booleanParam(defaultValue: false, name: 'SKIP_BUILD', description: 'Skip Build Step?')]
        env.SKIP_PR = INPUT_PARAMS.SKIP_PR;
        env.SKIP_DEV = INPUT_PARAMS.SKIP_DEV;
        env.SKIP_PROD = INPUT_PARAMS.SKIP_PROD;
        env.SKIP_TEST = INPUT_PARAMS.SKIP_TEST;
      }
      else {
        INPUT_PARAMS = input message: 'Build and deploy the latest changes?', ok: 'Yes',
            parameters: [
                booleanParam(defaultValue: false, name: 'SKIP_PR', description: 'Skip PR Deployment'),
                booleanParam(defaultValue: false, name: 'SKIP_BUILD', description: 'Skip Build Step?')]
        env.SKIP_PR = INPUT_PARAMS.SKIP_PR;
      }
      env.SKIP_BUILD = INPUT_PARAMS.SKIP_BUILD;


      echo "Confirming build and deploy of branch ${env.CHANGE_BRANCH}. SKIP_PR: ${INPUT_PARAMS.SKIP_PR},SKIP_DEV: ${INPUT_PARAMS.SKIP_DEV},SKIP_TEST: ${INPUT_PARAMS.SKIP_TEST},SKIP_PROD: ${INPUT_PARAMS.SKIP_PROD}, SKIP_BUILD: ${INPUT_PARAMS.SKIP_BUILD}."
  }
}
