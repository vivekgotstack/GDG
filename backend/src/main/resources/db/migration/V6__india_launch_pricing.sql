-- Preserve owner-customized plans. Only replace the original USD defaults.
UPDATE plan_definitions SET monthly_price=299,currency='INR',version=version+1 WHERE id='starter' AND monthly_price=7 AND currency='USD';
UPDATE plan_definitions SET monthly_price=599,currency='INR',version=version+1 WHERE id='studio' AND monthly_price=12 AND currency='USD';
UPDATE plan_definitions SET monthly_price=999,currency='INR',version=version+1 WHERE id='scale' AND monthly_price=20 AND currency='USD';
