The overall status of the RAO computation is exported in a **Reason** tag.
Here are the possible out-comes.

#### Secure network

~~~xml
<Reason>
    <code>Z13</code>
    <text>Network is secure</text>
</Reason>
~~~
This means that the RAO completed successfully and that the network is secure (i.e. no flow nor angle constraints remain).  

#### Unsecure network

~~~xml
<Reason>
    <code>Z03</code>
    <text>Network is unsecure</text>
</Reason>
~~~
This means that the RAO completed successfully but that the network is unsecure (i.e. there remains at least one flow or 
one angle constraints).

#### Load-flow divergence

~~~xml
<Reason>
    <code>B40</code>
    <text>Load flow divergence</text>
</Reason>
~~~
This means that the RAO or the angle monitoring could not be conducted normally because at least one perimeter lead to 
a load-flow divergence. This perimeter can be identified by looking at the [B57 Constraint_Series](#cnec-results).