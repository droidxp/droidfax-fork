#!/bin/bash

phaseone()
{
	# phase 1: pre-processing (static code analysis and instrumentation)
	echo -e "\n ###############   Phase 1: Code Analysis ############# \n"
	echo " INPUT "
	tree /home/hcai/testbed/demoinput/pairs

	bash instrAllPairs.sh demoinput explicit
	bash instrAllPairs.sh demoinput implicit 

	echo -e "\n Phase 1 finished \n"
	echo " OUTPUT "
	tree /home/hcai/testbed/cg.instrumented/demoinput/pairs
}

phasetwo()
{
	# phase 2: profiling (running instrumented apps to collect single- and inter-app traces)
	echo -e "\n ###############   Phase 2: Profiling ############# \n"
	echo " INPUT "
	tree /home/hcai/testbed/cg.instrumented/demoinput/pairs

	echo -e "\n ###############   2.1:  single-app Profiling ############# \n"
	bash runAllApps_monkey.sh demoinput explicit 60
	bash runAllApps_monkey.sh demoinput implicit 60

	echo -e "\n ###############   2.2:  inter-app Profiling ############# \n"
	bash runAllPairs_monkey.sh demoinput explicit 60
	bash runAllPairs_monkey.sh demoinput implicit 60

	echo -e "\n\n Phase 2 finished \n"
	killall -9 emulator
	echo " OUTPUT "
	tree /home/hcai/testbed/singleappTrace_*
	tree /home/hcai/testbed/pairTrace_*
}

phasethree()
{
	# phase 3: Characterization (computing the 122 characterization metrics)
	echo -e "\n ###############   Phase 3: Characterization ############# \n"
	echo " INPUT "
	tree /home/hcai/testbed/singleappTrace_*
	tree /home/hcai/testbed/pairTrace_*

	echo -e "\n ###############   3.1:  Computing General Metrics ############# \n"
	bash allGeneralReport.sh demoinput explicit demoresults
	bash allGeneralReport.sh demoinput implicit demoresults
	bash combineGeneralReport.sh demoresults/generalReport

	echo -e "\n ###############   3.2:  Computing ICC Metrics ############# \n"
	echo -e "\n ###############   3.2.1:  Computing Intra-app ICC Metrics ############# \n"
	bash allICCReport.sh demoinput explicit demoresults
	bash allICCReport.sh demoinput implicit demoresults
	bash combineICCReport.sh demoresults/ICCReport

	echo -e "\n ###############   3.2.2:  Computing Inter-app ICC Metrics ############# \n"
	bash allInterAppICCReport.sh demoinput explicit demoresults
	bash allInterAppICCReport.sh demoinput implicit demoresults
	bash combineInterAppICCReport.sh demoresults/interAppICCReport

	echo -e "\n ###############   3.3:  Computing Security Metrics ############# \n"
	bash allSecurityReport.sh demoinput explicit demoresults
	bash allSecurityReport.sh demoinput implicit demoresults
	bash combineSecurityReport.sh demoresults/securityReport

	echo -e "\n Phase 3 finished \n"
	echo " OUTPUT "
	tree /home/hcai/testbed/demoresults
}

phaseone

phasetwo

phasethree

echo -e "\n\n FINAL STEP: visualize and tabulate results\n\n"
bash distruteResScripts.sh demoresults
cd demoresults
bash produceall.sh


echo -e "\n ============== END OF RESULT REPRODUCTION (or DEMO) ============= "
echo -e "\n ==============  THANK YOU FOR YOUR TIME!  ============= "

exit 0

