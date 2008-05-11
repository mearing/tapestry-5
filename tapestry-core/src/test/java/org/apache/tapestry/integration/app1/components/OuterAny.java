// Copyright 2008 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.tapestry.integration.app1.components;

import org.apache.tapestry.annotations.Component;
import org.apache.tapestry.annotations.SupportsInformalParameters;
import org.apache.tapestry.corelib.components.Any;

@SupportsInformalParameters
public class OuterAny
{
    // Also, leave a few parameters here and there in the old naming style, with a
    // leading underscore.

    @Component(inheritInformalParameters = true)
    private Any innerAny;

}
