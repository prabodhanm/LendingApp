import { NgModule } from '@angular/core';

import { Myapp5SharedLibsModule, JhiAlertComponent, JhiAlertErrorComponent } from './';

@NgModule({
    imports: [Myapp5SharedLibsModule],
    declarations: [JhiAlertComponent, JhiAlertErrorComponent],
    exports: [Myapp5SharedLibsModule, JhiAlertComponent, JhiAlertErrorComponent]
})
export class Myapp5SharedCommonModule {}
