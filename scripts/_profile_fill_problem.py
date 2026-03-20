
import importlib.util, json, sys
from pathlib import Path
root = Path(r"D:/Code Projects/StandaloneCodePractice")
script_path = root / 'scripts' / 'expand_submission_suites_to_target.py'
spec = importlib.util.spec_from_file_location('expand_submission_suites_to_target', script_path)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
path = root / 'app/src/main/assets/neetcode150_catalog.json'
catalog = json.loads(path.read_text(encoding='utf-8'))
title = sys.argv[1]
for folder in catalog['folders']:
    for s in folder['sets']:
        for p in s['problems']:
            if p['title'] == title:
                added = module.fill_problem(p)
                print(json.dumps({'title': title, 'added': added, 'cases': len(p.get('submissionTestSuite',{}).get('cases',[]))}))
                raise SystemExit(0)
raise SystemExit(2)
